package com.alibaba.wisp.engine;


import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.UnsafeAccess;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

enum WispSysmon {
    INSTANCE;

    static {
        registerNatives();
    }

    private static Set<WispEngine> engines = new ConcurrentSkipListSet<>(new Comparator<WispEngine>() {
        @Override
        public int compare(WispEngine o1, WispEngine o2) {
            return o1.threadTask.compareTo(o2.threadTask);
        }
    });

    void startDaemon() {
        if (WispConfiguration.ENABLE_HANDOFF) {
            assert WispConfiguration.HANDOFF_POLICY != null;
            Thread thread = new Thread(WispEngine.daemonThreadGroup,
                    WispSysmon::sysmonLoop, "Wisp-Sysmon");
            thread.setDaemon(true);
            thread.start();
        }
    }

    void register(WispEngine engine) {
        if (WispConfiguration.ENABLE_HANDOFF) {
            engines.add(engine);
        }
    }

    private static void sysmonLoop() {
        final long interval = TimeUnit.MICROSECONDS.toNanos(WispConfiguration.SYSMON_TICK_US);
        long nextTick = System.nanoTime() + interval;
        while (true) {
            final long timeout = nextTick - System.nanoTime();
            if (timeout > 0) {
                UA.park0(false, timeout);
                handleLongOccupation();
            }
            nextTick += interval;
        }
    }

    /**
     * Handle a WispTask occupied a carrier thread for long time.
     */
    private static void handleLongOccupation() {
        for (WispEngine engine : engines) {
            if (engine.terminated) {
                // remove in iteration is OK for ConcurrentSkipListSet
                engines.remove(engine);
                continue;
            }
            if (engine.isRunning() && engine.schedTick == engine.lastSchedTick) {
                WispConfiguration.HANDOFF_POLICY.handle(engine);
            }
            engine.lastSchedTick = engine.schedTick;
        }
    }

    enum Policy {
        HAND_OFF { // handOff the carrier (Only supported in wisp2)
            @Override
            void handle(WispEngine engine) {
                if (JLA.isInSameNative(engine.thread)) {
                    engine.handOff();
                    engines.remove(engine);
                }
            }
        },
        PREEMPT { // insert a yield() after next safepoint
            @Override
            void handle(WispEngine engine) {
                markPreempted(engine.thread, true);
            }
        },
        ADAPTIVE { // depends on thread status
            @Override
            void handle(WispEngine engine) {
                if (JLA.isInSameNative(engine.thread)) {
                    engine.handOff();
                    engines.remove(engine);
                } else {
                    markPreempted(engine.thread, true);
                }
            }
        };

        abstract void handle(WispEngine engine);

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
