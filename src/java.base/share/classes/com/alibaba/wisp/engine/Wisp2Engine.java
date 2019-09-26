package com.alibaba.wisp.engine;


import java.dyn.CoroutineSupport;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
    private WispTask yieldingTask;
    private TimeOut pendingTimer;


    Wisp2Engine(Wisp2Group group) {
        this.group = group;
    }

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
        if (WispConfiguration.WISP_HIGH_PRECISION_TIMER && pendingTimer != null) {
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
        if (current.hasBeenShutdown) {
            return false;
        }
        assert current == WispEngine.current();
        assert !task.isThreadTask();
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

    private static boolean tryStealAndResumeTask(WispTask task, boolean failOnContention) {
        WispEngine current = WispEngine.current();
        /*
         * Please be extremely cautious:
         * task.engine can not be changed here by other thread
         * is base on our implementation of using park instead of
         * direct schedule, so only one thread could receive
         * this closure.
         */
        if (task.engine != current) {
            Wisp2Engine source = (Wisp2Engine) task.engine;
            if (!steal(task, current, failOnContention)) {
                return false;
            }
            WispEngine.TASK_COUNT_UPDATER.decrementAndGet(source);
            WispEngine.TASK_COUNT_UPDATER.incrementAndGet(current);
            // notify detached empty carrier to exit
            if (source.carrier.detached && WispEngine.TASK_COUNT_UPDATER.get(source) == 0) {
                source.carrier.signal();
            }
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
                if (!tryStealAndResumeTask(task, true)) {
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
    protected void doSchedule() {
        assert current.resumeEntry != null;
        current.resumeEntry.setStealEnable(true);
        yieldTo(threadTask); // letting the scheduler choose runnable task
    }


    @Override
    protected void yield() {
        if (!WispConfiguration.WISP_HIGH_PRECISION_TIMER && carrier != null) {
            carrier.processTimer();
        }
        if (WispEngine.runningAsCoroutine(current.getThreadWrapper())) {
            if (getTaskQueueLength() > 0) {
                assert yieldingTask == null;
                yieldingTask = current;
                // delay it, make sure wakeupTask is called after yield out
                schedule();
            }
        } else {
            JLA.yield0();
        }
    }

    private void processYield() {
        if (yieldingTask != null) {
            wakeupTask(yieldingTask);
            yieldingTask = null;
        }
    }

    @Override
    public void execute(Runnable target) {
        dispatchTask(target, "executor task", null);
    }

    @Override
    public void shutdown() {
        if (hasBeenShutdown) {
            return;
        }
        hasBeenShutdown = true;
        group.scheduler.executeWithCarrierThread(new StealAwareRunnable() {
            @Override
            public void run() {
                WispEngine current = current();
                current.runTaskInternal(new Runnable() {
                    @Override
                    public void run() {
                        notifyAndWaitTasksForShutdown(Wisp2Engine.this);
                    }
                }, WispTask.SHUTDOWN_TASK_NAME, null, null);
            }

            @Override
            public boolean isStealEnable() {
                return false;
            }
        }, thread);
    }

    @Override
    protected void dispatchTask(Runnable target, String name, Thread thread) {
        // wisp2 DO NOT ALLOW running task in non-scheduler thread
        ClassLoader ctxClassLoader = current.ctxClassLoader;
        long enqueueTime = getNanoTime();

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
        if (hasBeenShutdown) {
            return null;
        }
        WispTask task = group.groupTaskCache.poll();
        if (task == null) {
            return null;
        }
        if (task.engine != this) {
            if (!steal(task, this, true)) {
                group.groupTaskCache.add(task);
                return null;
            }
        }
        return task;
    }

    @Override
    void returnTaskToCache(WispTask task) {
        // reuse exited wispTasks from shutdown wispEngine is very tricky, so we'd better not return
        // these tasks to global cache
        if (taskCache.size() > WispConfiguration.WISP_ENGINE_TASK_CACHE_SIZE && !hasBeenShutdown) {
            group.groupTaskCache.add(task);
        } else {
            taskCache.add(task);
        }
    }

    @Override
    protected void registerEvent(WispTask target, SelectableChannel ch, int events) throws IOException {
        if (ch != null && ch.isOpen() && events != 0) {
            WispEventPump.INSTANCE.registerEvent(target, ch, events);
        }
    }

    @Override
    protected boolean isRunning() {
        return current != threadTask;
    }

    @Override
    protected int getTaskQueueLength() {
        if (carrier == null) {
            return 0;
        }
        int ql = carrier.queueLength;
        // use a local copy to avoid queueLength change to negative.
        return ql > 0 ? ql : 0;
    }

    @Override
    protected void runWispTaskEpilog() {
        processPendingTimer();
        processYield();
    }

    private void notifyAndWaitTasksForShutdown(Wisp2Engine engine) {
        // wait until current 'shutdown wispTask' is the only
        // running wispTask on this engine
        while (runningTaskCount != 1) {
            List<WispTask> runningTasks = getRunningTasks();
            for (WispTask task : runningTasks) {
                if (task.engine == this && task.isAlive()) {
                    task.interrupt();
                }
            }
            yield();
        }

        assert WispTask.SHUTDOWN_TASK_NAME.equals(current.getName());

        carrier.pushAndSignal(new StealAwareRunnable() {
            @Override
            public void run() {
                // set shutdownFinished true when SHUTDOWN-TASK finished
                // SHUTDOWN-TASK would keep interrupting running tasks until
                // it is the only running one
                assert runningTaskCount == 0;

                thread.getCoroutineSupport().drain();
                engine.shutdownFuture.countDown();

                //exit carrier
                carrier.detached = true;
            }
            @Override
            public boolean isStealEnable() {
                return false;
            }
        });
    }

    @Override
    protected void runTaskYieldEpilog() {
        if (hasBeenShutdown) {
            assert WispEngine.current().current.engine == this;
            if (WispTask.SHUTDOWN_TASK_NAME.equals(current.getName())
                || current == threadTask
                || CoroutineSupport.isInClinit(current.ctx)) {
                return;
            }
            UNSAFE.throwException(new ThreadDeath());
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
    }
}
