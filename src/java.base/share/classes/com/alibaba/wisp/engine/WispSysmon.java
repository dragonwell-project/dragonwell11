package com.alibaba.wisp.engine;


import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.UnsafeAccess;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

enum WispSysmon {
    INSTANCE;

    static {
        registerNatives();
    }

    private Set<WispEngine> engines = new ConcurrentSkipListSet<>(new Comparator<WispEngine>() {
        @Override
        public int compare(WispEngine o1, WispEngine o2) {
            return o1.threadTask.compareTo(o2.threadTask);
        }
    });
    final static String WISP_SYSMON_NAME = "Wisp-Sysmon";

    void startDaemon() {
        if (WispConfiguration.ENABLE_HANDOFF) {
            assert WispConfiguration.HANDOFF_POLICY != null;
            Thread thread = new Thread(WispEngine.daemonThreadGroup,
                    WispSysmon.INSTANCE::sysmonLoop, WISP_SYSMON_NAME);
            thread.setDaemon(true);
            thread.start();
        }
    }

    void register(WispEngine engine) {
        if (WispConfiguration.ENABLE_HANDOFF) {
            engines.add(engine);
        }
    }

    private void sysmonLoop() {
        final long interval = TimeUnit.MICROSECONDS.toNanos(WispConfiguration.SYSMON_TICK_US);
        final long carrierCheckRate = TimeUnit.MICROSECONDS.toNanos(WispConfiguration.SYSMON_CARRIER_GROW_TICK_US);
        final long checkCarrierOnNthTick = carrierCheckRate / interval;
        final boolean checkCarrier = WispConfiguration.CARRIER_GROW && checkCarrierOnNthTick > 0
                                    // Detach group's worker cnt is not specified by configuration
                                    && WispConfiguration.WORKER_COUNT == Runtime.getRuntime().availableProcessors();
        long nextTick = System.nanoTime() + interval;
        int tick = 0;

        while (true) {
            long timeout = nextTick - System.nanoTime();
            if (timeout > 0) {
                do {
                    UA.park0(false, timeout);
                } while ((timeout = nextTick - System.nanoTime()) > 0);
                handleLongOccupation();
                if (checkCarrier && tick++ == checkCarrierOnNthTick) {
                    Wisp2Group.WISP2_ROOT_GROUP.scheduler.checkAndGrowCarriers(Runtime.getRuntime().availableProcessors());
                    tick = 0;
                }
            } // else: we're too slow, skip a tick
            nextTick += interval;
        }
    }

    private List<WispEngine> longOccupationEngines = new ArrayList<>();

    /**
     * Handle a WispTask occupied a carrier thread for long time.
     */
    private void handleLongOccupation() {
        for (WispEngine engine : engines) {
            if (engine.terminated) {
                // remove in iteration is OK for ConcurrentSkipListSet
                engines.remove(engine);
                continue;
            }
            if (engine.isRunning() && engine.schedTick == engine.lastSchedTick) {
                longOccupationEngines.add(engine);
            }
            engine.lastSchedTick = engine.schedTick;
        }

        if (!longOccupationEngines.isEmpty()) {
            Iterator<WispEngine> itr = longOccupationEngines.iterator();
            while (itr.hasNext()) {
                WispEngine engine = itr.next();
                WispConfiguration.HANDOFF_POLICY.handle(engine, !itr.hasNext());
                itr.remove();
            }
        }
        assert longOccupationEngines.isEmpty();
    }

    enum Policy {
        HAND_OFF { // handOff the carrier (Only supported in wisp2)
            @Override
            void handle(WispEngine engine, boolean isLast) {
                if (JLA.isInSameNative(engine.thread)) {
                    engine.handOff();
                    INSTANCE.engines.remove(engine);
                }
            }
        },
        PREEMPT { // insert a yield() after next safepoint
            @Override
            void handle(WispEngine engine, boolean isLast) {
                markPreempted(engine.thread, isLast);
            }
        },
        ADAPTIVE { // depends on thread status
            @Override
            void handle(WispEngine engine, boolean isLast) {
                if (JLA.isInSameNative(engine.thread)) {
                    engine.handOff();
                    INSTANCE.engines.remove(engine);
                } else {
                    markPreempted(engine.thread, isLast);
                }
            }
        };

        abstract void handle(WispEngine engine, boolean isLast);

    }

    private static native void registerNatives();


    /**
     * Mark the thread as running single wispTask in java too much time.
     * And the Thread.yield() invocation will be emitted after next safepoint.
     *
     * @param thread the thread to mark
     * @param force  fire a force_safepoint immediately
     */
    private static native void markPreempted(Thread thread, boolean force);

    private static final UnsafeAccess UA = SharedSecrets.getUnsafeAccess();
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
}
