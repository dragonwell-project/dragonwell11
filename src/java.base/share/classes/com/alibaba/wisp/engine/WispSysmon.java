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
     *
     * the strategy is depends on carrier thread status:
     *
     * running java code: insert a yield() for next method return
     * running native code : handOff the carrier (Only supported in wisp2)
     *
     */
    private static void handleLongOccupation() {
        for (WispEngine engine : engines) {
            if (engine.terminated) {
                // remove in iteration is OK for ConcurrentSkipListSet
                engines.remove(engine);
            }
            if (engine.isRunning() && engine.schedTick == engine.lastSchedTick) {
                if (JLA.isInNative(engine.thread)) {
                    engine.handOff();
                } else {
                    markPreempt(engine.thread);
                }
            }

            engine.lastSchedTick = engine.schedTick;
        }
    }

    private static native void registerNatives();

    private static native void markPreempt(Thread thread);

    private static final UnsafeAccess UA = SharedSecrets.getUnsafeAccess();
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
}
