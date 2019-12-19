package com.alibaba.wisp.engine;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WispV2Group represents a series of {@link WispEngine}, which can steal
 * tasks from each other to achieve work-stealing.
 * <p>
 * {@code WispV2Group#WISP_ROOT_GROUP} is created by system.
 * {@link WispEngine#dispatch(Runnable)} in non-carrier thread and WISP_ROOT_GROUP's
 * carrier thread will dispatch task in this group.
 * <p>
 * User code could also create {@link WispGroup} by calling
 * {@link WispGroup#createGroup(int, ThreadFactory)},
 * Calling {@link WispGroup#execute(Runnable)} will dispatch
 * WispTask inner created group.
 * {@link WispEngine#dispatch(Runnable)} in a user created group will also
 * dispatch task in current group.
 */
public class WispGroup extends AbstractExecutorService {

    private static final String WISP_ROOT_GROUP_NAME = "Root";

    static final WispGroup WISP_ROOT_GROUP = new WispGroup(WISP_ROOT_GROUP_NAME);

    final WispScheduler scheduler;
    final Set<WispEngine> carrierEngines;
    final Queue<WispTask> groupTaskCache = new ConcurrentLinkedQueue<>();
    final CyclicBarrier shutdownBarrier;

    /**
     * Create a new WispV2Group for executing tasks.
     *
     * @param size carrier thread counter
     * @param tf   ThreadFactory used to create carrier thread
     */
    public static WispGroup createGroup(int size, ThreadFactory tf) {
        return new WispGroup(size, tf);
    }

    /**
     * Create Root Carrier.
     */
    private WispGroup(String name) {
        carrierEngines = new ConcurrentSkipListSet<>();
        // detached group won't shut down
        shutdownBarrier = null;
        scheduler = new WispScheduler(
                WispConfiguration.WORKER_COUNT,
                WispConfiguration.WISP_SCHEDULE_STEAL_RETRY,
                WispConfiguration.WISP_SCHEDULE_PUSH_RETRY,
                WispConfiguration.WISP_SCHEDULE_HELP_STEAL_RETRY,
                new ThreadFactory() {
                    AtomicInteger seq = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "Wisp-" + name + "-Carrier-" + seq.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                }, this, true);
    }

    private WispGroup(int size, ThreadFactory tf) {
        carrierEngines = new ConcurrentSkipListSet<>();
        shutdownBarrier = new CyclicBarrier(size);
        scheduler = new WispScheduler(size, tf, this);
    }

    public List<Long> getWispEngineIDs() {
        List<Long> workers = new ArrayList<>();
        for (WispEngine worker : carrierEngines) {
            workers.add(worker.getId());
        }
        return workers;
    }

    @Override
    public void shutdown() {
        assert this != WispGroup.WISP_ROOT_GROUP;
        for (WispEngine worker : carrierEngines) {
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
        for (WispEngine worker : carrierEngines) {
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
