package com.alibaba.wisp.engine;

import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.WispEngineAccess;

import java.dyn.CoroutineExitException;
import java.dyn.CoroutineSupport;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

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
public abstract class WispEngine extends AbstractExecutorService {

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

    public static boolean enableSocketLock() {
        return WispConfiguration.WISP_ENABLE_SOCKET_LOCK;
    }

    @Deprecated
    public static boolean isTransparentAsync() {
        return transparentWispSwitch();
    }

    @Deprecated
    public static boolean isShiftThreadModel() {
        return shiftThreadModel;
    }

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
    /*
     * Wisp specified Thread Group
     * all the daemon threads in wisp should be created with the Thread Group.
     * In Thread.start(), if the thread should not convert to WispTask,
     * check whether the thread's group is daemonThreadGroup
     */
    static ThreadGroup daemonThreadGroup;

    static Set<Thread> carrierThreads;

    static {
        registerNatives();
        setWispEngineAccess();
    }

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
            if (WispConfiguration.WISP_VERSION == 2) {
                Wisp2Group.WISP2_ROOT_GROUP.scheduler.startCarrierThreads();
            }
            // TODO: move poller thread start in future patch
        }
    }

    private static void setWispEngineAccess() {
        SharedSecrets.setWispEngineAccess(new WispEngineAccess() {
            @Override
            public WispTask getCurrentTask() {
                return WispEngine.current().getCurrentTask();
            }

            @Override
            public void dispatch(Runnable runnable, String name) {
                WispEngine engine = current();
                engine.dispatchTask(runnable, name, null);
            }

            @Override
            public void eventLoop() {
                WispEngine.eventLoop();
            }

            @Override
            public void registerEvent(SelectableChannel ch, int events) throws IOException {
                WispEngine.current().registerEvent(ch, events);
            }

            @Override
            public void registerEpollEvent(int epFd) throws IOException {
                assert WispConfiguration.WISP_VERSION == 2;
                WispEngine.current().registerEpollEvent(epFd);
            }

            @Override
            public boolean usingWispEpoll(Thread t) {
                return WispConfiguration.WISP_VERSION == 2 && runningAsCoroutine(t);
            }

            @Override
            public void unregisterEvent() {
                WispEngine.current().unregisterEvent();
            }

            @Override
            public void yieldOnBlocking() {
                WispEngine.current().yieldOnBlocking();
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
            public <T> T runInCritical(CheckedSupplier<T> supplier) {
                WispEngine engine = null;
                boolean critical0 = false;
                if (WispEngine.transparentWispSwitch()) {
                    engine = WispEngine.current();
                    critical0 = engine.isInCritical;
                    engine.isInCritical = true;
                }
                try {
                    return supplier.get();
                } catch (Throwable t) {
                    UNSAFE.throwException(t);
                    return null; // make compiler happy
                } finally {
                    if (engine != null) {
                        engine.isInCritical = critical0;
                    }
                }
            }

            @Override
            public boolean hasMoreTasks() {
                return WispEngine.current().getTaskQueueLength() > 0;
            }

            @Override
            public boolean runningAsCoroutine(Thread t) {
                return WispEngine.runningAsCoroutine(t);
            }

            public boolean isAlive(WispTask task) {
                return task.isAlive();
            }

            @Override
            public void interrupt(WispTask task) {
                task.interrupt();
            }

            @Override
            public boolean isInterrupted(WispTask task) {
                return task.isInterrupted();
            }

            @Override
            public boolean testInterruptedAndClear(WispTask task, boolean clear) {
                return task.testInterruptedAndClear(clear);
            }

            @Override
            public boolean ifPutToCurrentEngine() {
                return WispConfiguration.ifPutToCurrentEngine();
            }

            @Override
            public boolean ifProxySelector() {
                return WispConfiguration.ifProxySelector();
            }

            @Override
            public boolean ifSpinSelector() {
                return WispConfiguration.ifSpinSelector();
            }

            @Override
            public boolean ifPutToManagedThread() {
                return WispConfiguration.ifPutToManagedThread();
            }

            @Override
            public boolean useThreadPoolLimit() {
                return WispConfiguration.USE_THREAD_POOL_LIMIT;
            }

            @Override
            public String getThreadUsage(String threadName) {
                return WispWorkerContainer.getThreadUsage(threadName);
            }

            private final static String JAVA_LANG = "java.lang";

            private final static String JDK_INTERNAL = "jdk.internal";

            /**
             * Try to "start thread" as wisp if all listed condition is satisfied:
             *
             * 1. not in java.lang package
             * 2. not a WispEngine Thread (may overlapping 2; except user created wisp2 carrier)
             * 3. allThreadAsWisp is true or call stack satisfies wisp.conf's description.
             *
             * @param thread the thread
             * @param target thread's target field
             *
             * @return if condition is satisfied and thread is started as wisp
             */
            @Override
            public boolean tryStartThreadAsWisp(Thread thread, Runnable target) {
                if (thread.getClass().getName().startsWith(JAVA_LANG) &&
                        (target == null || target.getClass().getName().startsWith(JAVA_LANG))) {
                    return false;
                }
                if (thread.getClass().getName().startsWith(JDK_INTERNAL) &&
                        (target == null || target.getClass().getName().startsWith(JDK_INTERNAL))) {
                    return false;
                }
                if (WispEngine.isEngineThread(thread) ||
                        !(WispConfiguration.ALL_THREAD_AS_WISP || WispConfiguration.ifPutToManagedThread())) {
                    return false;
                }
                // pthread_create always return before new thread started, so we should not wait here
                JLA.setWispAlive(thread, true); // thread.isAlive() should be true
                WispWorkerContainer.INSTANCE.dispatch(getThreadUsage(thread.getName()), thread.getName(), thread, thread);
                return true;
            }
        });
    }

    private static void initializeClasses() {
        try {
            Class.forName(CoroutineExitException.class.getName());
            Class.forName(WispThreadWrapper.class.getName());
            if (WispConfiguration.GLOBAL_POLLER) {
                Class.forName(WispPoller.class.getName());
            }
            WispEngine.current().preloadClasses();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static boolean isEngineThread(Thread t) {
        assert daemonThreadGroup != null;
        return daemonThreadGroup == t.getThreadGroup() || carrierThreads.contains(t);
    }

    static boolean runningAsCoroutine(Thread t) {
        if (WispConfiguration.WISP_VERSION == 1) {
            // For wisp1:
            // Any thread could create WispTask, we need
            // hook all blocking API to avoid WispTask blocking each other
            return true;
        }
        assert WispConfiguration.WISP_VERSION == 2;

        WispTask task = t == null ? WispEngine.current().getCurrentTask() : JLA.getWispTask(t);
        assert task != null;
        // For wisp2:
        // Only carrierThread could create WispTask, and
        // the carrierThread will listen on WispTask's wakeup.
        // So we can safely letting the threadTask block the whore Thread.
        return !task.isThreadTask();
    }


    WispEngine() {
        thread = JLA.currentThread0();
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
            engine = WispConfiguration.WISP_VERSION == 2 ?
                    new Wisp2Engine(Wisp2Group.WISP2_ROOT_GROUP) :
                    new ScheduledWispEngine();
            if (engine.threadTask.ctx != null) {
                JLA.setWispEngine(thread, engine);
                engine.init();
            } // else: fake engine used in jni attach
        }
        return engine;
    }

    protected final Thread thread;
    // current running task
    protected WispTask current;
    protected final WispTask threadTask;
    protected List<WispTask> taskCache = new ArrayList<>();

    private int createdTasks;
    protected volatile int runningTaskCount = 0;

    protected boolean isInCritical;
    private boolean hasBeenShutdown;
    // do not need volatile, because we do force enqueue after
    // `hasBeenShutdown` becomes true, the flag could always be seen

    Statistics statistics = new Statistics();

    WispCounter counter;
    int schedTick;
    int lastSchedTick; // access by Sysmon
    boolean terminated;
    private long switchTimestamp = 0;

    /**
     * Entry point for running an engine.
     * Please make sure calling {@code WispTask.createTask(...)} somewhere, so WispTasks were created before call
     * {@code eventLoop()}
     * <p>
     * A typical server looks like this:
     * <pre>
     *  WispEngine.dispatch(() -> {
     *      while (client = accept()) {
     *          WispEngine.dispatch(
     *                  () -> handle(client), "client handler");
     *      }
     *  }, "accepter");
     *
     *  WispEngine.eventLoop(); // loop forever
     * <pre/>
     */
    private static void eventLoop() {
        WispEngine engine = current();
        while (engine.runningTaskCount > 0) {
            engine.yieldToNext();
        }
    }

    /**
     * @return Currently running WispTask. Ensured by {@link #yieldTo(WispTask)}
     * If calling in a non-coroutine environment, return a thread-emulated WispTask.
     */
    WispTask getCurrentTask() {
        return current;
    }

    WispCounter getCounter() {
        return counter;
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

    final WispTask runTaskInternal(Runnable target, String name, Thread thread, ClassLoader ctxLoader) {
        if (hasBeenShutdown) {
            return null;
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
     * Wake up a {@link WispTask} that belongs to this engine,
     * and caller will also do a {@link Selector#wakeup()} if engine's thread is being blocked on Selector.
     *
     * @param task target task
     */
    boolean wakeupTask(WispTask task) {
        return wakeupTask(task, false);
    }

    /**
     * Block current coroutine and do scheduling.
     * Typically called when resource is not ready.
     */
    protected void yieldOnBlocking() {
        current.countExecutionTime(switchTimestamp);
        if (current.status == WispTask.Status.RUNNABLE) {
            current.status = WispTask.Status.BLOCKED;
        }

        if (current.parent != null && current.parent.isRunnable()) {
            WispTask parent = current.parent;
            assert parent.engine == this;
            // DESIGN:
            // only the first park of wisp should go back to parent
            current.parent = null;
            yieldTo(parent);
        } else {
            yieldToNext();
        }
        if (hasBeenShutdown) {
            doShutdown();
        }
    }

    /**
     * The ONLY entry point to a task,
     * {@link #current} will be set correctly
     *
     * @param task coroutine to run
     */
    protected boolean yieldTo(WispTask task) {
        if (task == null) {
            return false;
        }
        schedTick++;

        assert task.engine == this;

        if (task == current) {
            task.status = WispTask.Status.RUNNABLE;
            switchTimestamp = getNanoTimeForProfile();
            return true;
        }

        if (task.status == WispTask.Status.BLOCKED)
            task.status = WispTask.Status.RUNNABLE;

        if (task.status == WispTask.Status.ZOMBIE) {
            unregisterEvent(task);
            return false;
        }

        WispTask from = current;
        current = task;
        counter.incrementSwitchCount();
        assert !isInCritical;
        WispTask.switchTo(from, task);
        assert !WispEngine.current().isInCritical;
        current.engine.switchTimestamp = getNanoTimeForProfile();
        return true;
    }


    /**
     * 1. In Wisp2, each WispEngine's runningTask is modified when WispTask is stolen, we can't guarantee
     * the accuracy of the task set.
     * 2. this function is only called in shutdown, so it's not performance sensitive
     * 3. this function should only be called by current WispTask
     */
    protected List<WispTask> getRunningTasks() {
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


    /**
     * Modify current {@link WispTask}'s interest channel and event.
     * {@see registerEvent(...)}
     * <p>
     * Used for implementing socket io
     * <pre>
     *     while (!ch.read(buf) == 0) { // 0 indicate IO not ready, not EOF..
     *         registerEvent(ch, OP_READ);
     *         yieldOnBlocking();
     *     }
     *     // read is done here
     * <pre/>
     */
    private void registerEvent(SelectableChannel ch, int events) throws IOException {
        registerEvent(current, ch, events);
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
            try {
                registerEvent(target, target.ch, 0);
                target.resetRegisterEventTime();
                target.ch = null;
            } catch (IOException e) {
                // pass
            }
        }
    }

    /**
     * The only exit path of a task.
     * WispTask must call {@code taskExit()} to exit safely.
     */
    void taskExit() { // and exit
        current.status = WispTask.Status.ZOMBIE;
        TASK_COUNT_UPDATER.decrementAndGet(this);

        current.countExecutionTime(switchTimestamp);
        current.resetThreadWrapper();
        switchTimestamp = 0;

        unregisterEvent();
        returnTaskToCache(current);

        counter.incrementCompleteTaskCount();

        if (runningTaskCount == 0 && threadTask.isRunnable()) {
            // finish the event loop
            yieldTo(threadTask);
        } else {

            // In Tenant killing process, we have an pending exception,
            // WispTask.Coroutine's loop will be break
            // invoke an explicit reschedule instead of return
            yieldOnBlocking();
        }
    }

    private void destroy() {
        WispTask.cleanExitedTasks(taskCache);
        WispTask.cleanExitedTask(threadTask);
        closeEngineSelector();
        terminated = true;
    }


    // ----------------------------------------------- initialization

    /**
     * Preload wisp runtime used class.
     * Only called when WispEngine is loading.
     */
    protected abstract void preloadClasses() throws Exception;


    // ----------------------------------------------- lifecycle hooks

    /**
     * hook for leave wispTask entry
     */
    protected void runWispTaskEpilog() {
    }


    // ----------------------------------------------- timer related

    /**
     * Add a timer for current {@link WispTask},
     * used for implementing timed IO operation / sleep etc...
     *
     * @param deadlineNano deadline of the timer
     * @param fromJvm      synchronized or obj.wait()
     */
    protected abstract void addTimer(long deadlineNano, boolean fromJvm);


    /**
     * Cancel the timer added by {@link #addTimer(long, boolean)}.
     */
    protected abstract void cancelTimer();


    // -----------------------------------------------  yielding

    /**
     * yield to next runnable task.
     */
    protected abstract void yieldToNext();

    /**
     * Telling to the scheduler that the current thread is willing to yield
     * its current use of a processor.
     * <p>
     * Called by {@link Thread#yield()}
     */
    protected abstract void yield();
    // -----------------------------------------------  event related

    /**
     * register target {@link WispTask}'s interest channel and event.
     *
     * @param ch     the channel that is related to the current WispTask
     * @param events interest event
     * @throws IOException
     */
    protected abstract void registerEvent(WispTask target, SelectableChannel ch, int events) throws IOException;

    /**
     * register target {@link WispTask}'s interest epoll fd
     *
     * @param epFd the epoll fd that is related to the current WispTask
     * @throws IOException
     */
    protected void registerEpollEvent(int epFd) throws IOException {
        throw new UnsupportedOperationException();
    }

    // -----------------------------------------------  task related

    /**
     * Create a wisp task and run.
     *
     * @param target the task to execute
     * @param name   task name
     */
    protected abstract void dispatchTask(Runnable target, String name, Thread thread);

    /**
     * Wake up a {@link WispTask} that belongs to Wisp Implement,
     * and caller may need call {@link Selector#wakeup()} if engine's thread is being blocked on Selector.
     *
     * @param task   target task
     * @param urgent give the task a high priority to be schedule, not promised for all implementation
     */
    protected abstract boolean wakeupTask(WispTask task, boolean urgent);

    /**
     * @return task from global cached theScheduler
     */
    abstract WispTask getTaskFromCache();

    /**
     * return task back to global cache
     */
    abstract void returnTaskToCache(WispTask task);

    /**
     * create a Entry runnable for wisp task,
     * used for bridge coroutine and Executor interface.
     */
    StealAwareRunnable createResumeEntry(WispTask task) {
        return null;
    }


    // -----------------------------------------------  poller


    /**
     * clean resource
     */
    protected void closeEngineSelector() {
    }

    // ----------------------------------------------- status fetch

    /**
     * @return if current engine is busy
     */
    protected abstract boolean isRunning();

    /**
     * @return queue length. used for mxBean report
     */
    protected abstract int getTaskQueueLength();

    /**
     * @return running task number, used for mxBean report
     */
    int getNumberOfrunningTaskCount() {
        return runningTaskCount;
    }

    // -----------------------------------------------  shutdown support

    /**
     * start up shutdown process, the typical implement is
     * wake up thread coroutine, then send exception to coroutines
     * one by one.
     */
    protected abstract void startShutdown();

    /**
     * Send exception to coroutines one by one.
     * This function should noly be called in WispEngine.doShutdown.
     */
    protected abstract void iterateTasksForShutdown();

    // -----------------------------------------------  retake

    /**
     * hand off wispEngine for blocking system calls.
     */
    protected void handOff() {
    }

    static long getNanoTimeForProfile() {
        return WispConfiguration.WISP_PROFILE_DETAIL ? System.nanoTime() : 0;
    }

    void countEnqueueTime(long enqueueTime) {
        if (enqueueTime != 0) {
            counter.incrementTotalEnqueueTime(System.nanoTime() - enqueueTime);
        }
    }

    long exponentialSmoothed(long accumulator, long newVal) {
        return (accumulator * 88 + newVal * 12) / 100;
    }

    class Statistics {
        int maxRunning;
        int maxReschedule;
        int doubleCheckWakeup;

        int eventLoops;
        int nonBlockSelectCount;
        int selectNothingActive;
        int selectNothingPassive;
        int proxyBlockingSelect;
        int proxySelectNow;
        int ioEventCount;

        int doIO;
        int doWakeup;
        int doTimer;

        // not precision, because modified by multi-thread
        int wakeupCount;
        int realWakeupCount;
        int innerEngineWakeup;
        int alreadyWakeup;

        int selectorRebuild;
        int retryCanceledKey;
    }

    @Override
    public String toString() {
        return new SimpleDateFormat("MM-dd HH:mm:ss.SSS").format(new Date()) + "\n" +
                "Engine (" + thread.getName() + ") Runtime Info:" +
                "\nrunningTaskCount\t" + runningTaskCount +
                "\ncreatedTasks\t" + createdTasks +
                "\neventLoops\t" + statistics.eventLoops +
                "\nswitchCount\t" + schedTick;
    }

    @Override
    public void shutdown() {
        hasBeenShutdown = true;
        if (WispEngine.current().current == threadTask) {
            doShutdown();
        } else {
            startShutdown();
        }
    }

    private CountDownLatch shutdownFuture = new CountDownLatch(1);

    void doShutdown() {
        if (current == threadTask) {
            iterateTasksForShutdown();
            thread.getCoroutineSupport().drain();
            shutdownFuture.countDown();
        } else {
            UNSAFE.throwException(new ThreadDeath());
        }
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

    protected static final AtomicIntegerFieldUpdater<WispEngine> TASK_COUNT_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(WispEngine.class, "runningTaskCount");

    private static native void registerNatives();

    private static native int getProxyUnpark(int[] res);
}
