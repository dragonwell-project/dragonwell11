package com.alibaba.wisp.engine;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * In wisp1 WispTask could run in ANY user created thread.
 * Wisp2 thread model has been changed to java.util.concurrent.Executor
 * <p>
 * The new thread model bring some restrictions to us, brings us listed benefit:
 * <ol>
 * <li>Work load could be steal by other threads in Executor</li>
 * <li>Eliminate park/unpark API hook for non coroutine thread</li>
 * </ol>
 */
final class Wisp2Engine extends WispEngine {

    private static final ScheduledExecutorService timer = !WispConfiguration.WISP_HIGH_PRECISION_TIMER ? null :
            Executors.newScheduledThreadPool(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(WispEngine.daemonThreadGroup, r);
                    thread.setDaemon(true);
                    thread.setName("Wisp2-Timer");
                    return thread;
                }
            });

    private static Queue<WispTask> globalTaskCache = new ConcurrentLinkedQueue<>();

    @Override
    protected void preloadClasses() throws Exception {
        if (WispConfiguration.WISP_HIGH_PRECISION_TIMER) {
            timer.submit(new Runnable() {
                @Override
                public void run() {
                }
            });
        }
        addTimer(System.nanoTime() + Integer.MAX_VALUE, false);
        cancelTimer();
    }

    Wisp2Group group;
    Wisp2Scheduler.Carrier carrier;

    Wisp2Engine(Wisp2Group group) {
        this.group = group;
    }

    @Override
    protected void postInit() {
        runningTasks = new ConcurrentSkipListSet<>(); // support concurrent modify
    }

    private TimeOut pendingTimer;

    @Override
    protected void addTimer(long deadlineNano, boolean fromJvm) {
        WispTask task = current;
        TimeOut timeOut = new TimeOut(task, deadlineNano, fromJvm);
        task.timeOut = timeOut;

        if (WispConfiguration.WISP_HIGH_PRECISION_TIMER) {
            if (task.isThreadTask()) {
                scheduleInTimer(timeOut);
            } else {
                /*
                 * timer.schedule may enter park() again
                 * we delegate this operation to thread coroutine
                 * (which always use native park)
                 */
                pendingTimer = timeOut;
            }
        } else {
            group.scheduler.addTimer(timeOut, thread);
        }
    }

    private void processPendingTimer() {
        if (pendingTimer != null) {
            scheduleInTimer(pendingTimer);
            pendingTimer = null;
        }
    }

    private void scheduleInTimer(TimeOut timeOut) {
        boolean isInCritical0 = isInCritical;
        final long timeout = timeOut.deadlineNano - System.nanoTime();
        isInCritical = true;
        if (timeout > 0) {
            timer.schedule(new Runnable() {
                @Override
                public void run() {
                    if (!timeOut.canceled) {
                        timeOut.doUnpark();
                    }
                }
            }, timeout, TimeUnit.NANOSECONDS);
        } else if (!timeOut.canceled) {
            timeOut.task.jdkUnpark();
        }
        isInCritical = isInCritical0;
    }

    @Override
    protected void cancelTimer() {
        if (current.timeOut != null) {
            current.timeOut.canceled = true;
            group.scheduler.cancelTimer(current.timeOut, thread);
            current.timeOut = null;
        }
        pendingTimer = null;
    }

    /**
     * Steal task from it's carrier engine to current
     *
     * @param failOnContention steal fail if there's too much lock contention
     * @return if success
     */
    private static boolean steal(WispTask task, WispEngine current, boolean failOnContention) {
        assert current == WispEngine.current();
        if (task.engine != current) {
            while (task.stealLock != 0) {/* wait until steal enabled */}
            assert task.status != WispTask.Status.RUNNABLE;
            assert task.parent == null;
            if (!task.ctx.steal(failOnContention)) {
                task.stealFailureCount++;
                return false;
            }
            task.stealCount++;
            task.engine = current;
        }
        return true;
    }

    private static boolean resumeTask(WispTask task, boolean failOnContention) {
        WispEngine current = WispEngine.current();
        /*
         * Please be extremely cautious:
         * task.engine can not be changed here by other thread
         * is base on our implementation of using park instead of
         * direct yieldOnBlocking, so only one thread could receive
         * this closure.
         */
        if (task.engine != current) {
            WispEngine source = task.engine;
            if (!steal(task, current, failOnContention)) {
                return false;
            }
            source.runningTasks.remove(task);
            current.runningTasks.add(task);
        }
        current.countEnqueueTime(task.getEnqueueTime());
        task.resetEnqueueTime();
        current.yieldTo(task);
        current.runWispTaskEpilog();
        return true;
    }

    @Override
    StealAwareRunnable createResumeEntry(WispTask task) {
        assert !task.isThreadTask();
        return new StealAwareRunnable() {
            boolean stealEnable = true;
            @Override
            public void run() {
                if (!resumeTask(task, true)) {
                    stealEnable = false;
                    wakeupTask(task);
                }
            }

            @Override
            public void setStealEnable(boolean b) {
                stealEnable = b;
            }

            @Override
            public boolean isStealEnable() {
                return stealEnable;
            }
        };
    }

    /**
     * @param task   target task
     * @param urgent not used
     */
    @Override
    protected boolean wakeupTask(WispTask task, boolean urgent) {
        assert !task.isThreadTask();
        assert task.resumeEntry != null;
        task.updateEnqueueTime();
        group.scheduler.executeWithCarrierThread(task.resumeEntry, task.engine.thread);
        return true;
    }

    @Override
    protected void yieldToNext() {
        assert current.resumeEntry != null;
        current.resumeEntry.setStealEnable(true);
        yieldTo(threadTask); // letting the scheduler choose runnable task
    }

    @Override
    protected void yield() {
        if (WispEngine.runningAsCoroutine(current.getThreadWrapper())) {
            wakeupTask(current);
            // can be steal, and the stealing thread may
            // blocking on `stealingLock`
            // Not a big problem, because we'll release it
            // immediately
            yieldOnBlocking();
        } else {
            JLA.yield0();
        }
    }

    @Override
    public void execute(Runnable target) {
        dispatchTask(target, "executor task", null);
    }

    @Override
    protected void dispatchTask(Runnable target, String name, Thread thread) {
        // wisp2 DO NOT ALLOW running task in non-scheduler thread
        ClassLoader ctxClassLoader = current.ctxClassLoader;
        long enqueueTime = getNanoTimeForProfile();

        group.scheduler.execute(new StealAwareRunnable() {
            @Override
            public void run() {
                WispEngine current = WispEngine.current();
                current.countEnqueueTime(enqueueTime);
                current.runTaskInternal(target, name, thread, ctxClassLoader);
            }
        });
    }

    @Override
    WispTask getTaskFromCache() {
        if (!taskCache.isEmpty()) {
            return taskCache.remove(taskCache.size() - 1);
        }
        WispTask task = globalTaskCache.poll();
        if (task == null) {
            return null;
        }
        if (task.engine != this) {
            if (!steal(task, this, true)) {
                globalTaskCache.add(task);
                return null;
            }
        }
        return task;
    }

    @Override
    void returnTaskToCache(WispTask task) {
        if (taskCache.size() > WispConfiguration.WISP_ENGINE_TASK_CACHE_SIZE) {
            globalTaskCache.add(task);
        } else {
            taskCache.add(task);
        }
    }

    @Override
    protected void registerEvent(WispTask target, SelectableChannel ch, int events) throws IOException {
        if (ch != null && ch.isOpen() && events != 0) {
            WispPoller.INSTANCE.registerEvent(target, ch, events);
        }
    }

    @Override
    protected void registerEpollEvent(int epFd) throws IOException {
        WispPoller.INSTANCE.registerEvent(current, epFd, SelectionKey.OP_READ);
    }

    @Override
    protected boolean isRunning() {
        return current != threadTask;
    }

    @Override
    protected int getTaskQueueLength() {
        return carrier == null ? 0 : (carrier.queueLength > 0 ? carrier.queueLength : 0);
    }

    @Override
    protected void runWispTaskEpilog() {
        processPendingTimer();
    }

    @Override
    protected void startShutdown() {
        group.scheduler.executeWithCarrierThread(new StealAwareRunnable() {
            @Override
            public void run() {
                doShutdown();
            }

            @Override
            public boolean isStealEnable() {
                return false;
            }
        }, thread);
    }

    @Override
    protected void iterateTasksForShutdown() {
        while (!runningTasks.isEmpty()) {
            yieldTo(runningTasks.iterator().next());
        }
    }

    /**
     * Current retake strategy is:
     * 1. allocate a new WispEngine as the copy of the blocked engine
     * 2. handOff the blocked engine
     * 2. Let other engine steal all tasks
     */
    @Override
    protected void handOff() {
        group.scheduler.handOffCarrierThread(thread);
        // this carrier can not steal task from now on,
        // runningTasks will never grow
        for (WispTask task : runningTasks) {
            if (current != task) {
                group.scheduler.execute(new StealAwareRunnable() {
                    @Override
                    public void run() {
                        // avoiding steal failure by lock contention
                        if (!resumeTask(task, false)) {
                            // The task will be remained in the handoffed thread.
                            // We already make our best efforts.
                            // Forcedly stealing a steal-disabled task which contains
                            // an unexpected native frame will result in jvm crash

                            // retry is meaningless, because the carrier thread is occupied
                            // by the running task, and other tasks has no chance to move on
                        }
                    }
                });
            }
        }
    }
}
