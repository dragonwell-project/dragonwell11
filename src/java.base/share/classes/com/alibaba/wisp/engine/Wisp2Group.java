package com.alibaba.wisp.engine;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WispV2Group represents a series of {@link Wisp2Engine}, which can steal
 * tasks from each other to achieve work-stealing.
 * <p>
 * {@code WispV2Group#WISP2_ROOT_GROUP} is created by system.
 * {@link WispEngine#dispatch(Runnable)} in non-carrier thread and WISP2_ROOT_GROUP's
 * carrier thread will dispatch task in this group.
 * <p>
 * User code could also create {@link Wisp2Group} by calling
 * {@link Wisp2Group#createGroup(int, ThreadFactory)},
 * Calling {@link Wisp2Group#execute(Runnable)} will dispatch
 * WispTask inner created group.
 * {@link WispEngine#dispatch(Runnable)} in a user created group will also
 * dispatch task in current group.
 */
public class Wisp2Group extends AbstractExecutorService {

    private static final String WISP2_ROOT_GROUP_NAME = "Root";

    static final Wisp2Group WISP2_ROOT_GROUP = new Wisp2Group(WISP2_ROOT_GROUP_NAME);

    final Wisp2Scheduler scheduler;
    final Set<Wisp2Engine> carrierEngines;
    final Queue<WispTask> groupTaskCache = new ConcurrentLinkedDeque<>();
    final CyclicBarrier shutdownBarrier;

    /**
     * Create a new WispV2Group for executing tasks.
     *
     * @param size carrier thread counter
     * @param tf   ThreadFactory used to create carrier thread
     */
    public static Wisp2Group createGroup(int size, ThreadFactory tf) {
        return new Wisp2Group(size, tf);
    }

    private static Set<Wisp2Engine> createEngineSet() {
        return new ConcurrentSkipListSet<>(new Comparator<Wisp2Engine>() {
            @Override
            public int compare(Wisp2Engine o1, Wisp2Engine o2) {
                return o1.threadTask.compareTo(o2.threadTask);
            }
        });
    }

    /**
     * Create Root Carrier.
     */
    private Wisp2Group(String name) {
        carrierEngines = createEngineSet();
        // detached group won't shut down
        shutdownBarrier = null;
        scheduler = new Wisp2Scheduler(
                WispConfiguration.WORKER_COUNT,
                WispConfiguration.WISP_SCHEDULE_STEAL_RETRY,
                WispConfiguration.WISP_SCHEDULE_PUSH_RETRY,
                WispConfiguration.WISP_SCHEDULE_HELP_STEAL_RETRY,
                new ThreadFactory() {
                    AtomicInteger seq = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Wisp2-" + name + "-Carrier-" + seq.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                }, this, true);
    }

    private Wisp2Group(int size, ThreadFactory tf) {
        carrierEngines = createEngineSet();
        shutdownBarrier = new CyclicBarrier(size);
        scheduler = new Wisp2Scheduler(size, tf, this);
    }

    public List<Long> getWispEngineIDs() {
        List<Long> workers = new ArrayList<>();
        for (Wisp2Engine worker : carrierEngines) {
            workers.add(worker.getId());
        }
        return workers;
    }

    @Override
    public void shutdown() {
        assert this != Wisp2Group.WISP2_ROOT_GROUP;
        for (Wisp2Engine worker : carrierEngines) {
            worker.shutdown();
        }

        for (WispTask task : groupTaskCache) {
            WispTask.cleanExitedTask(task);
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return carrierEngines.iterator().next().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return carrierEngines.stream().allMatch(WispEngine::isTerminated);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        for (Wisp2Engine worker : carrierEngines) {
            long t = deadline - System.nanoTime();
            if (t <= 0 || !worker.awaitTermination(t, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void execute(Runnable command) {
        long enqueueTime = WispEngine.getNanoTime();
        scheduler.execute(new StealAwareRunnable() {
            @Override
            public void run() {
                WispEngine engine = WispEngine.current();
                engine.countEnqueueTime(enqueueTime);
                engine.runTaskInternal(command, "group dispatch task",
                        null, engine.current.ctxClassLoader);
            }
        });
    }
}
