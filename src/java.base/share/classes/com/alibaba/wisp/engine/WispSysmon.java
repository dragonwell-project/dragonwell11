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

    private Set<WispCarrier> carriers = new ConcurrentSkipListSet<>();
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

    void register(WispCarrier carrier) {
        if (WispConfiguration.ENABLE_HANDOFF) {
            carriers.add(carrier);
        }
    }

    private void sysmonLoop() {
        final long interval = TimeUnit.MICROSECONDS.toNanos(WispConfiguration.SYSMON_TICK_US);
        final long carrierCheckRate = TimeUnit.MICROSECONDS.toNanos(WispConfiguration.SYSMON_CARRIER_GROW_TICK_US);
        final long checkCarrierOnNthTick = carrierCheckRate / interval;
        final boolean checkCarrier = WispConfiguration.CARRIER_GROW && checkCarrierOnNthTick > 0
                // Detach carrier's worker cnt is not specified by configuration
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
                    WispEngine.WISP_ROOT_ENGINE.scheduler.checkAndGrowWorkers(Runtime.getRuntime().availableProcessors());
                    tick = 0;
                }
            } // else: we're too slow, skip a tick
            nextTick += interval;
        }
    }

    private List<WispCarrier> longOccupationEngines = new ArrayList<>();

    /**
     * Handle a WispTask occupied a worker thread for long time.
     */
    private void handleLongOccupation() {
        for (WispCarrier carrier : carriers) {
            if (carrier.terminated) {
                // remove in iteration is OK for ConcurrentSkipListSet
                carriers.remove(carrier);
                continue;
            }
            if (carrier.isRunning() && carrier.schedTick == carrier.lastSchedTick) {
                longOccupationEngines.add(carrier);
            }
            carrier.lastSchedTick = carrier.schedTick;
        }

        if (!longOccupationEngines.isEmpty()) {
            Iterator<WispCarrier> itr = longOccupationEngines.iterator();
            while (itr.hasNext()) {
                WispCarrier carrier = itr.next();
                WispConfiguration.HANDOFF_POLICY.handle(carrier, !itr.hasNext());
                itr.remove();
            }
        }
        assert longOccupationEngines.isEmpty();
    }

    enum Policy {
        HAND_OFF { // handOff the worker
            @Override
            void handle(WispCarrier carrier, boolean isLast) {
                if (JLA.isInSameNative(carrier.thread)) {
                    carrier.handOff();
                    INSTANCE.carriers.remove(carrier);
                }
            }
        },
        PREEMPT { // insert a yield() after next safepoint
            @Override
            void handle(WispCarrier carrier, boolean isLast) {
                markPreempted(carrier.thread, isLast);
            }
        },
        ADAPTIVE { // depends on thread status
            @Override
            void handle(WispCarrier carrier, boolean isLast) {
                if (JLA.isInSameNative(carrier.thread)) {
                    carrier.handOff();
                    INSTANCE.carriers.remove(carrier);
                } else {
                    markPreempted(carrier.thread, isLast);
                }
            }
        };

        abstract void handle(WispCarrier carrier, boolean isLast);

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
