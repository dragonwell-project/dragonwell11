package com.alibaba.wisp.engine;

import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.WispEngineAccess;
import sun.nio.ch.SelChImpl;

import java.dyn.Coroutine;
import java.dyn.CoroutineExitException;
import java.dyn.CoroutineSupport;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Coroutine Runtime Engine. It's a "wisp" thing, as we want our asynchronization transformation to be transparent
 * without asking a Java programmer to modify his/her code.
 * <p>
 * <p> {@link WispTask} provides high-level semantics of {link @Coroutine}.
 * {@link WispTask} is created, managed and scheduled by {@link WispEngine}.
 * <p>
 * <p> A {@link WispEngine} instance is expected to run in a specific thread. Get per-thread instance by calling
 * {@link WispEngine#current()}.
 */
public class WispEngine
        extends AbstractExecutorService
        implements Comparable<WispEngine> {
    /*
     some of our users change this field by reflection
     in the runtime to disable wisp temporarily.
     We should move shiftThreadModel to WispConfiguration
     after we provide api to control this behavior and
     notify the users to modify their code.

     TODO refactor to com.alibaba.wisp.enableThreadAsWisp later
     */
    private static boolean shiftThreadModel;

    public static boolean transparentWispSwitch() {
        return WispConfiguration.TRANSPARENT_WISP_SWITCH;
    }

    public static boolean enableThreadAsWisp() {
        return shiftThreadModel;
    }

    @Deprecated
    public static boolean isTransparentAsync() {
        return transparentWispSwitch();
    }

    @Deprecated
    public static boolean isShiftThreadModel() {
        return shiftThreadModel;
    }

    static final Unsafe UNSAFE = Unsafe.getUnsafe();
    static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
    /*
     * Wisp specified Thread Group
     * all the daemon threads in wisp should be created with the Thread Group.
     * In Thread.start(), if the thread should not convert to WispTask,
     * check whether the thread's group is daemonThreadGroup
     */
    static ThreadGroup daemonThreadGroup;

    static Set<Thread> carrierThreads;
    private static Thread pollerThread;
    static Map<Long, WispCounter> managedEngineCounters;

    private static final ScheduledExecutorService timer = !WispConfiguration.WISP_HIGH_PRECISION_TIMER ? null :
            Executors.newScheduledThreadPool(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(WispEngine.daemonThreadGroup, r);
                    thread.setDaemon(true);
                    thread.setName("Wisp-Timer");
                    return thread;
                }
            });

    private static void initializeWispClass() {
        assert JLA != null : "WispEngine should be initialized after System";
        assert JLA.currentThread0().getName().equals("main") : "Wisp need to be loaded by main thread";
        shiftThreadModel = WispConfiguration.ENABLE_THREAD_AS_WISP;
        daemonThreadGroup = new ThreadGroup(JLA.currentThread0().getThreadGroup(), "Daemon Thread Group");
        carrierThreads = new ConcurrentSkipListSet<>(new Comparator<Thread>() {
            @Override
            public int compare(Thread o1, Thread o2) {
                return Long.compare(o1.getId(), o2.getId());
            }
        });
        if (transparentWispSwitch()) {
            initializeClasses();
            JLA.wispBooted();
        }
    }

    private static void startWispDaemons() {
        if (transparentWispSwitch()) {
            Thread unparker = new Thread(daemonThreadGroup, new Runnable() {
                @Override
                public void run() {
                    int[] proxyUnparks = new int[12];
                    CoroutineSupport.setWispBooted();
                    while (true) {
                        int n = getProxyUnpark(proxyUnparks);
                        for (int i = 0; i < n; i++) {
                            WispTask task = WispTask.fromId(proxyUnparks[i]);
                            if (task != null) {
                                task.unpark();
                            } // else: target engine exited
                        }
                    }
                }
            }, "Wisp-Unpark-Dispatcher");
            unparker.setDaemon(true);
            unparker.start();
            WispSysmon.INSTANCE.startDaemon();
            WispGroup.WISP_ROOT_GROUP.scheduler.startCarrierThreads();

            if (!WispConfiguration.CARRIER_AS_POLLER) {
                pollerThread = new Thread(daemonThreadGroup, new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            WispEventPump.INSTANCE.pollAndDispatchEvents(-1);
                        }
                    }
                }, "Wisp-Poller");
                pollerThread.setDaemon(true);
                pollerThread.start();
            }
            if (WispConfiguration.WISP_PROFILE_LOG_ENABLED) {
                WispPerfCounterMonitor.INSTANCE.startDaemon();
            }
        }
    }

    private static void setWispEngineAccess() {
        SharedSecrets.setWispEngineAccess(new WispEngineAccess() {

            @Override
            public WispTask getCurrentTask() {
                return WispEngine.current().getCurrentTask();
            }

            @Override
            public void registerEvent(SelectableChannel ch, int events) throws IOException {
                WispEngine.current().registerEvent(ch, events);
            }

            @Override
            public void unregisterEvent() {
                WispEngine.current().unregisterEvent();
            }

            @Override
            public int epollWait(int epfd, long pollArray, int arraySize, long timeout,
                                 AtomicReference<Object> status, Object INTERRUPTED) throws IOException {
                return WispEventPump.INSTANCE.epollWaitForWisp(epfd, pollArray, arraySize, timeout, status, INTERRUPTED);
            }

            @Override
            public void interruptEpoll(AtomicReference<Object> status, Object INTERRUPTED, int interruptFd) {
                WispEventPump.INSTANCE.interruptEpoll(status, INTERRUPTED, interruptFd);
            }

            @Override
            public void addTimer(long deadlineNano) {
                WispEngine.current().addTimer(deadlineNano, false);
            }

            @Override
            public void cancelTimer() {
                WispEngine.current().cancelTimer();
            }

            @Override
            public void sleep(long ms) {
                WispTask.sleep(ms);
            }

            @Override
            public void yield() {
                WispEngine.current().yield();
            }

            @Override
            public boolean isThreadTask(WispTask task) {
                return task.isThreadTask();
            }

            @Override
            public boolean isTimeout() {
                WispTask task = WispEngine.current().current;
                return task.timeOut != null && task.timeOut.expired();
            }

            @Override
            public void park(long timeoutNano) {
                WispTask.jdkPark(timeoutNano);
            }

            @Override
            public void unpark(WispTask task) {
                if (task != null) {
                    task.jdkUnpark();
                }
            }

            @Override
            public void destroy() {
                WispEngine.current().destroy();
            }

            @Override
            public boolean hasMoreTasks() {
                return WispEngine.current().getTaskQueueLength() > 0;
            }

            @Override
            public boolean runningAsCoroutine(Thread t) {
                return WispEngine.runningAsCoroutine(t);
            }

            @Override
            public boolean usingWispEpoll() {
                return runningAsCoroutine(null);
            }

            public boolean isAlive(WispTask task) {
                return task.isAlive();
            }

            @Override
            public void interrupt(WispTask task) {
                task.interrupt();
            }

            @Override
            public boolean testInterruptedAndClear(WispTask task, boolean clear) {
                return task.testInterruptedAndClear(clear);
            }


            @Override
            public boolean isAllThreadAsWisp() {
                return WispConfiguration.ALL_THREAD_AS_WISP;
            }

            @Override
            public boolean tryStartThreadAsWisp(Thread thread, Runnable target) {
                return ThreadAsWisp.tryStart(thread, target);
            }

            @Override
            public boolean useDirectSelectorWakeup() {
                return WispConfiguration.USE_DIRECT_SELECTOR_WAKEUP;
            }

            @Override
            public boolean enableSocketLock() {
                return WispConfiguration.WISP_ENABLE_SOCKET_LOCK;
            }

            @Override
            public StackTraceElement[] getStackTrace(WispTask task) {
                return task.getStackTrace();
            }
        });
    }

    private static void initializeClasses() {
        try {
            Class.forName(CoroutineExitException.class.getName());
            Class.forName(WispThreadWrapper.class.getName());
            if (WispConfiguration.ENABLE_THREAD_AS_WISP) {
                Class.forName(ThreadAsWisp.class.getName());
            }
            Class.forName(WispEventPump.class.getName());
            managedEngineCounters = new ConcurrentHashMap<>(100);
            if (WispConfiguration.WISP_PROFILE) {
                Class.forName(WispPerfCounterMonitor.class.getName());
            }
            if (WispConfiguration.WISP_HIGH_PRECISION_TIMER) {
                timer.submit(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
            WispEngine.current().addTimer(System.nanoTime() + Integer.MAX_VALUE, false);
            WispEngine.current().cancelTimer();
            Coroutine.StealResult.SUCCESS.ordinal();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static boolean isEngineThread(Thread t) {
        assert daemonThreadGroup != null;
        return daemonThreadGroup == t.getThreadGroup() || carrierThreads.contains(t);
    }

    static boolean runningAsCoroutine(Thread t) {
        WispTask task = t == null ? WispEngine.current().getCurrentTask() : JLA.getWispTask(t);
        assert task != null;
        // For wisp2:
        // Only carrierThread could create WispTask, and
        // the carrierThread will listen on WispTask's wakeup.
        // So we can safely letting the non-carrier wispTask block the whore Thread.
        return !task.isThreadTask();
    }

    /**
     * Constructor is private, so one can only get thread-specific engine by calling this method.
     * This method's name indicates a task is created in current thread's engine.
     * <p>
     * We can not use ThreadLocal any more, because if transparentAsync, it behaves as a coroutine local.
     *
     * @return thread-specific engine
     */
    public static WispEngine current() {
        Thread thread = JLA.currentThread0();
        WispEngine engine = JLA.getWispEngine(thread);
        if (engine == null) {
            engine = new WispEngine(WispGroup.WISP_ROOT_GROUP);
            if (engine.threadTask.ctx != null) {
                JLA.setWispEngine(thread, engine);
                engine.init();
            } // else: fake engine used in jni attach
        }
        return engine;
    }

    final Thread thread;
    // current running task
    WispTask current;
    WispGroup group;
    WispScheduler.Carrier carrier;

    private final WispTask threadTask;
    private List<WispTask> taskCache = new ArrayList<>();

    private int createdTasks;
    volatile int runningTaskCount = 0;

    boolean isInCritical;
    volatile boolean hasBeenShutdown;

    WispCounter counter;
    int schedTick;
    int lastSchedTick; // access by Sysmon
    boolean terminated;
    private long switchTimestamp = 0;
    CountDownLatch shutdownFuture = new CountDownLatch(1);

    private WispTask yieldingTask;
    private TimeOut pendingTimer;


    private WispEngine(WispGroup group) {
        thread = JLA.currentThread0();
        this.group = group;
        CoroutineSupport cs = thread.getCoroutineSupport();
        current = threadTask = new WispTask(this,
                cs == null ? null : cs.threadCoroutine(),
                cs != null, true);
        if (cs == null) { // fake engine used in jni attach
            threadTask.setThreadWrapper(thread);
        } else {
            threadTask.reset(null, null,
                    "THREAD: " + thread.getName(), thread, thread.getContextClassLoader());
        }
    }

    /**
     * Use 2nd-phase init after constructor. Because if constructor calls Thread.currentThread(),
     * and recursive calls constructor, then stackOverflow.
     */
    private void init() {
        WispTask.trackTask(threadTask);
        counter = WispCounter.create(this);
    }

    /**
     * @return Currently running WispTask. Ensured by {@link #yieldTo(WispTask)}
     * If calling in a non-coroutine environment, return a thread-emulated WispTask.
     */
    WispTask getCurrentTask() {
        return current;
    }

    /**
     * Each WispEngine has a corresponding thread. Thread can't be changed for WispEngine.
     * Use thread id as WispEngine id.
     * @return WispEngine id
     */
    public long getId() {
        assert thread != null;
        return thread.getId();
    }

    public static Map<Long, WispCounter> getManagedEngineCounters() {
        return managedEngineCounters;
    }

    /**
     * Create WispTask to run task code
     * <p>
     * The real running thread depends on implementation
     *
     * @param target target code
     */
    public static void dispatch(Runnable target) {
        WispEngine engine = current();
        engine.dispatchTask(target, "dispatch task", null);
    }

    /**
     * Create a wisp task and run.
     *
     * @param target the task to execute
     * @param name   task name
     */
    void dispatchTask(Runnable target, String name, Thread thread) {
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

    final WispTask runTaskInternal(Runnable target, String name, Thread thread, ClassLoader ctxLoader) {
        if (hasBeenShutdown && !WispTask.SHUTDOWN_TASK_NAME.equals(name)) {
            throw new RejectedExecutionException("Wisp engine has been shutdown");
        }
        boolean isInCritical0 = isInCritical;
        isInCritical = true;
        WispTask wispTask;
        try {
            if (0 == createdTasks++) {
                WispSysmon.INSTANCE.register(this);
            }
            counter.incrementCreateTaskCount();
            if ((wispTask = getTaskFromCache()) == null) {
                wispTask = new WispTask(this, null, true, false);
                WispTask.trackTask(wispTask);
            }
            wispTask.reset(target, current, name, thread, ctxLoader);
            TASK_COUNT_UPDATER.incrementAndGet(this);
        } finally {
            isInCritical = isInCritical0;
        }
        yieldTo(wispTask);
        runWispTaskEpilog();

        return wispTask;
    }


    /**
     * The only exit path of a task.
     * WispTask must call {@code taskExit()} to exit safely.
     */
    void taskExit() { // and exit
        current.status = WispTask.Status.ZOMBIE;
        TASK_COUNT_UPDATER.decrementAndGet(this);

        current.countExecutionTime(switchTimestamp);
        switchTimestamp = 0;

        unregisterEvent();
        returnTaskToCache(current);

        // reset threadWrapper after call returnTaskToCache,
        // since the threadWrapper will be used in Thread.currentThread()
        current.resetThreadWrapper();
        counter.incrementCompleteTaskCount();

        // In Tenant killing process, we have an pending exception,
        // WispTask.Coroutine's loop will be break
        // invoke an explicit reschedule instead of return
        schedule();
    }

    /**
     * @return task from global cached theScheduler
     */
    private WispTask getTaskFromCache() {
        assert WispEngine.current() == this;
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
            if (steal(task) != Coroutine.StealResult.SUCCESS) {
                group.groupTaskCache.add(task);
                return null;
            }
        }
        assert task.engine == this;
        return task;
    }

    /**
     * return task back to global cache
     */
    private void returnTaskToCache(WispTask task) {
        // reuse exited wispTasks from shutdown wispEngine is very tricky, so we'd better not return
        // these tasks to global cache
        if (taskCache.size() > WispConfiguration.WISP_ENGINE_TASK_CACHE_SIZE && !hasBeenShutdown) {
            group.groupTaskCache.add(task);
        } else {
            taskCache.add(task);
        }
    }

    /**
     * hook for yield wispTask
     */
    private void runWispTaskEpilog() {
        processPendingTimer();
        processYield();
    }

    private void destroy() {
        WispTask.cleanExitedTasks(taskCache);
        WispTask.cleanExitedTask(threadTask);
        terminated = true;
    }

    /**
     * Block current coroutine and do scheduling.
     * Typically called when resource is not ready.
     */
    protected final void schedule() {
        assert WispEngine.current() == this;
        WispTask current = this.current;
        current.countExecutionTime(switchTimestamp);
        WispTask parent = current.parent;
        if (parent != null) {
            assert parent.isRunnable();
            assert parent.engine == this;
            // DESIGN:
            // only the first park of wisp should go back to parent
            current.parent = null;
            yieldTo(parent);
        } else {
            assert current.resumeEntry != null && current != threadTask
                    : "call `schedule()` in scheduler";
            current.resumeEntry.setStealEnable(true);
            yieldTo(threadTask); // letting the scheduler choose runnable task
        }
        if (hasBeenShutdown && current != threadTask
                && !WispTask.SHUTDOWN_TASK_NAME.equals(current.getName())) {
            CoroutineSupport.checkAndThrowException(current.ctx);
        }
    }

    /**
     * Wake up a {@link WispTask} that belongs to this engine
     *
     * @param task target task
     */
    void wakeupTask(WispTask task) {
        assert !task.isThreadTask();
        assert task.resumeEntry != null;
        assert task.engine == this;
        task.updateEnqueueTime();
        group.scheduler.executeWithCarrierThread(task.resumeEntry, thread);
    }

    /**
     * create a Entry runnable for wisp task,
     * used for bridge coroutine and Executor interface.
     */
    StealAwareRunnable createResumeEntry(WispTask task) {
        assert !task.isThreadTask();
        return new StealAwareRunnable() {
            boolean stealEnable = true;

            @Override
            public void run() {
                WispEngine current = WispEngine.current();
                /*
                 * Please be extremely cautious:
                 * task.engine can not be changed here by other thread
                 * is base on our implementation of using park instead of
                 * direct schedule, so only one thread could receive
                 * this closure.
                 */
                WispEngine source = task.engine;
                if (source != current) {
                    Coroutine.StealResult res = current.steal(task);
                    if (res != Coroutine.StealResult.SUCCESS) {
                        if (res != Coroutine.StealResult.FAIL_BY_CONTENTION) {
                            stealEnable = false;
                        }
                        source.wakeupTask(task);
                        return;
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
     * Steal task from it's carrier engine to this engine
     *
     * @return steal result
     */
    private Coroutine.StealResult steal(WispTask task) {
        /* shutdown is an async operation in wisp2, SHUTDOWN task relies on runningTaskCount to
        determine whether it's okay to exit the carrier, hence we need to make sure no more new
        wispTasks are created or stolen for hasBeenShutdown engines
        for example:
        1. SHUTDOWN task found runningTaskCount equals 0 and exit
        2. carrier's task queue may still has some remaining tasks, when tried to steal these tasks
        we may encounter jvm crash.
        TODO:// merge shutdown and handoff dependencies check
        */
        if (hasBeenShutdown || task.engine.hasBeenShutdown) {
            return Coroutine.StealResult.FAIL_BY_STATUS;
        }
        assert WispEngine.current() == this;
        assert !task.isThreadTask();
        if (task.engine != this) {
            while (task.stealLock != 0) {/* wait until steal enabled */}
            assert task.parent == null;
            Coroutine.StealResult res = task.ctx.steal(true);
            if (res != Coroutine.StealResult.SUCCESS) {
                task.stealFailureCount++;
                return res;
            }
            task.stealCount++;
            task.setEngine(this);
        }
        return Coroutine.StealResult.SUCCESS;
    }

    /**
     * The ONLY entry point to a task,
     * {@link #current} will be set correctly
     *
     * @param task coroutine to run
     */
    private final boolean yieldTo(WispTask task) {
        assert task != null;
        assert WispEngine.current() == this;
        assert task.engine == this;
        assert task != current;

        if (task.status == WispTask.Status.ZOMBIE) {
            unregisterEvent(task);
            return false;
        }

        WispTask from = current;
        current = task;
        counter.incrementSwitchCount();
        switchTimestamp = getNanoTime();
        assert !isInCritical;
        WispTask.switchTo(from, task);
        // Since engine is changed with stealing, we shouldn't directly access engine's member any more.
        assert WispEngine.current().current == from;
        assert !from.engine.isInCritical;
        return true;
    }


    /**
     * Telling to the scheduler that the current thread is willing to yield
     * its current use of a processor.
     * <p>
     * Called by {@link Thread#yield()}
     */
    void yield() {
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
        assert current.isThreadTask();
        if (yieldingTask != null) {
            wakeupTask(yieldingTask);
            yieldingTask = null;
        }
    }



    /**
     * Modify current {@link WispTask}'s interest channel and event.
     * {@see registerEvent(...)}
     * <p>
     * Used for implementing socket io
     * <pre>
     *     while (!ch.read(buf) == 0) { // 0 indicate IO not ready, not EOF..
     *         registerEvent(ch, OP_READ);
     *         schedule();
     *     }
     *     // read is done here
     * <pre/>
     */
    private void registerEvent(SelectableChannel ch, int events) throws IOException {
        registerEvent(current, ch, events);
    }

    /**
     * register target {@link WispTask}'s interest channel and event.
     *
     * @param ch     the channel that is related to the current WispTask
     * @param events interest event
     * @throws IOException
     */
    private void registerEvent(WispTask target, SelectableChannel ch, int events) throws IOException {
        if (ch != null && ch.isOpen() && events != 0) {
            WispEventPump.INSTANCE.registerEvent(target, ((SelChImpl) ch).getFDVal(), events);
        }
    }
    /**
     * Clean current task's interest event before an non-IO blocking operation
     * or task exit to prevent unexpected wake up.
     */
    void unregisterEvent() {
        unregisterEvent(current);
    }

    private void unregisterEvent(WispTask target) {
        if (target.ch != null) {
            target.resetRegisterEventTime();
            target.ch = null;
        }
    }

    /**
     * Add a timer for current {@link WispTask},
     * used for implementing timed IO operation / sleep etc...
     *
     * @param deadlineNano deadline of the timer
     * @param fromJvm      synchronized or obj.wait()
     */
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


    /**
     * Cancel the timer added by {@link #addTimer(long, boolean)}.
     */
    protected void cancelTimer() {
        if (current.timeOut != null) {
            current.timeOut.canceled = true;
            group.scheduler.cancelTimer(current.timeOut, thread);
            current.timeOut = null;
        }
        pendingTimer = null;
    }

    private void processPendingTimer() {
        assert current.isThreadTask();
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

    /**
     * @return if current engine is busy
     */
    boolean isRunning() {
        return current != threadTask;
    }

    /**
     * @return queue length. used for mxBean report
     */
    int getTaskQueueLength() {
        if (carrier == null) {
            return 0;
        }
        int ql = carrier.queueLength;
        // use a local copy to avoid queueLength change to negative.
        return ql > 0 ? ql : 0;
    }

    /**
     * @return running task number, used for mxBean report
     */
    int getRunningTaskCount() {
        return runningTaskCount;
    }

    // -----------------------------------------------  shutdown support

    @Override
    public void shutdown() {
        if (hasBeenShutdown) {
            return;
        }
        hasBeenShutdown = true;
        deRegisterPerfCounter();
        group.scheduler.executeWithCarrierThread(new StealAwareRunnable() {
            @Override
            public void run() {
                WispEngine current = current();
                current.runTaskInternal(new Runnable() {
                    @Override
                    public void run() {
                        notifyAndWaitTasksForShutdown(WispEngine.this);
                    }
                }, WispTask.SHUTDOWN_TASK_NAME, null, null);
            }

            @Override
            public boolean isStealEnable() {
                return false;
            }
        }, thread);
    }

    private void notifyAndWaitTasksForShutdown(WispEngine engine) {
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

                try {
                    group.shutdownBarrier.await();
                } catch (Exception e) {
                    System.out.println("[Wisp] unexpected barrier exception " + e + ", thread: " + thread);
                }
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

    /**
     * 1. In Wisp2, each WispEngine's runningTask is modified when WispTask is stolen, we can't guarantee
     * the accuracy of the task set.
     * 2. this function is only called in shutdown, so it's not performance sensitive
     * 3. this function should only be called by current WispTask
     */
    private List<WispTask> getRunningTasks() {
        assert WispEngine.current() == this;
        ArrayList<WispTask> runningTasks = new ArrayList<>();
        boolean isInCritical0 = isInCritical;
        isInCritical = true;
        try {
            for (WispTask task : WispTask.id2Task.values()) {
                if (task.engine == this && !task.isThreadTask()) {
                    runningTasks.add(task);
                }
            }
            return runningTasks;
        } finally {
            isInCritical = isInCritical0;
        }
    }

    void handOff() {
        group.scheduler.handOffCarrierThread(thread);
    }

    // ----------------------------------------------- Monitoring

    WispCounter getCounter() {
        return counter;
    }

    static public WispCounter getWispCounter(long id) {
        return WispConfiguration.WISP_PROFILE ? WispPerfCounterMonitor.INSTANCE.getWispCounter(id) : null;
    }

    void registerPerfCounter() {
        runInCritical(() -> {
            if (WispConfiguration.WISP_PROFILE) {
                WispPerfCounterMonitor.INSTANCE.register(counter);
            }
            managedEngineCounters.put(getId(), counter);
            return null;
        });
    }

    void deRegisterPerfCounter() {
        runInCritical(() -> {
            if (WispConfiguration.WISP_PROFILE) {
                WispPerfCounterMonitor.INSTANCE.deRegister(counter);
            }
            managedEngineCounters.remove(getId());
            counter.cleanup();
            return null;
        });
    }

    // -----------------------------------------------  retake

    static long getNanoTime() {
        return WispConfiguration.WISP_PROFILE ? System.nanoTime() : 0;
    }

    void countEnqueueTime(long enqueueTime) {
        if (enqueueTime != 0) {
            counter.incrementTotalEnqueueTime(System.nanoTime() - enqueueTime);
        }
    }

    @Override
    public String toString() {
        return "WispEngine on " + thread.getName();
    }

    @Override
    public void execute(Runnable command) {
        dispatchTask(command, "execute task", null);
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return hasBeenShutdown;
    }

    @Override
    public boolean isTerminated() {
        return runningTaskCount == 0;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return shutdownFuture.await(timeout, unit);
    }

    @Override
    public int compareTo(WispEngine o) {
        return Long.compare(getId(), o.getId());
    }


    /*** DO NOT use this helper inside WispEngine,
     * because lambda may cause class loading.
     */
    static <T> T runInCritical(Supplier<T> supplier) {
        WispEngine engine = WispEngine.current();
        boolean critical0 = engine.isInCritical;
        engine.isInCritical = true;
        try {
            return supplier.get();
        } finally {
            engine.isInCritical = critical0;
            }
    }


    protected static final AtomicIntegerFieldUpdater<WispEngine> TASK_COUNT_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(WispEngine.class, "runningTaskCount");

    private static native void registerNatives();

    private static native int getProxyUnpark(int[] res);

    static {
        registerNatives();
        setWispEngineAccess();
    }
}
