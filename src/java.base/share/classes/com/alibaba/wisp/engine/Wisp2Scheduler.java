package com.alibaba.wisp.engine;

import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.UnsafeAccess;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;


/**
 * Wisp2 work-stealing implementation.
 * <p>
 * Every carrier thread has a {@link ConcurrentLinkedQueue} to
 * receive tasks.
 * Once local queue is empty, carrier will scan
 * others' queue, execute stolen task, then check local queue.
 * Again and again, until all the queue is empty.
 */
class Wisp2Scheduler {

    private final static int IDX_MASK = 0xffff; // ensure positive
    private final static int STEAL_HIGH_WATER_LEVEL = 8;

    // instance const
    private final int PARALLEL;
    private final int STEAL_RETRY;
    private final int PUSH_RETRY;
    private final int HELP_STEAL_RETRY;

    // carriers could be changed by handOff(),
    // add volatile to avoiding carriers's elements
    // to be allocated in register.
    // we could not add volatile volatile modifier
    // to array elements, so make the array volatile.
    private volatile Carrier[] carriers;
    private final ThreadFactory threadFactory;
    private final Wisp2Group group;

    Wisp2Scheduler(int parallelism, ThreadFactory threadFactory, Wisp2Group group) {
        this(parallelism, Math.max(1, parallelism / 2), parallelism,
                Math.max(1, parallelism / 4), threadFactory, group, true);
    }

    Wisp2Scheduler(int parallelism, int stealRetry, int pushRetry, int helpStealRetry,
                   ThreadFactory threadFactory, Wisp2Group group, boolean startThreads) {
        PARALLEL = parallelism;
        STEAL_RETRY = stealRetry;
        PUSH_RETRY = pushRetry;
        HELP_STEAL_RETRY = helpStealRetry;
        this.group = group;
        this.threadFactory = threadFactory;
        carriers = new Carrier[PARALLEL];
        for (int i = 0; i < parallelism; i++) {
            carriers[i] = new Carrier();
        }
        if (startThreads) {
            startCarrierThreads();
        }
    }

    void startCarrierThreads() {
        for (Carrier carrier : carriers) {
            carrier.thread.start();
        }
    }

    class Carrier implements Runnable {
        ConcurrentLinkedQueue<StealAwareRunnable> taskQueue;
        private final TimeOut.TimerManager timerManager;
        private final Thread thread;
        volatile boolean detached = false;

        private final static int QL_IDLE_PROCESSING_TIMER = -1;
        private final static int QL_IDLE = -2;
        volatile int queueLength;

        Carrier() {
            thread = threadFactory.newThread(this);
            WispEngine.carrierThreads.add(thread);
            taskQueue = new ConcurrentLinkedQueue<>();
            timerManager = new TimeOut.TimerManager();
            queueLength = 0;
        }

        void copyContextFromDetachedCarrier(Carrier detachedCarrier) {
            // copy timers
            timerManager.copyTimer(detachedCarrier.timerManager.queue);
            // drain wispTasks
            StealAwareRunnable task;
            while ((task = detachedCarrier.taskQueue.poll()) != null) {
                pushAndSignal(task);
            }
        }

        void runCarrier(WispEngine engine) {
            int r = (int) System.nanoTime();
            Runnable task;
            while (true) {
                while ((task = pollTask(false)) != null) {
                    doExec(task);
                }

                if (detached) {
                    if (engine.runningTaskCount == 0) {
                        return;
                    }
                } else if ((task = trySteal(STEAL_RETRY, r = nextRandom(r))) != null) {
                    doExec(task);
                    continue; // process local queue
                }

                if (queueLength == 0 && LENGTH_UPDATER.compareAndSet(this, 0, QL_IDLE_PROCESSING_TIMER)) {
                    long nanos = timerManager.processTimeoutEventsAndGetWaitNanos();
                    assert nanos != 0;
                    boolean doPark = false;
                    if (queueLength == QL_IDLE_PROCESSING_TIMER &&
                            (doPark = LENGTH_UPDATER.compareAndSet(this, QL_IDLE_PROCESSING_TIMER, QL_IDLE))
                            && taskQueue.peek() == null) {
                        UA.park0(false, nanos < 0 ? 0 : nanos);
                    }
                    LENGTH_UPDATER.addAndGet(this, doPark ? -QL_IDLE : -QL_IDLE_PROCESSING_TIMER);
                }
            }
        }

        @Override
        public void run() {
            try {
                Wisp2Engine engine = (Wisp2Engine) WispEngine.current();
                engine.group = group;
                engine.carrier = this;
                group.carrierEngines.add(engine);
                runCarrier(engine);
            } finally {
                WispEngine.carrierThreads.remove(thread);
            }
        }

        boolean idle() {
            return queueLength == QL_IDLE;
        }

        Runnable pollTask(boolean isSteal) {
            StealAwareRunnable task = taskQueue.poll();
            if (task != null) {
                if (isSteal && !task.isStealEnable()) {
                    // disable steal is a very uncommon case,
                    // The overhead here is acceptable
                    taskQueue.offer(task);
                    return null;
                }
                LENGTH_UPDATER.decrementAndGet(this);
            }
            return task;
        }

        /**
         * @return if it is idle
         */
        boolean pushAndSignal(StealAwareRunnable task) {
            taskQueue.offer(task);
            return doSignalIfNecessary(LENGTH_UPDATER.getAndIncrement(this));
        }

        void signal() {
            doSignalIfNecessary(queueLength);
        }

        /**
         * @return if it is idle
         */
        private boolean doSignalIfNecessary(int len) {
            if (len == QL_IDLE) {
                UA.unpark0(thread);
            }
            return len < 0;
        }

        Wisp2Scheduler theScheduler() {
            return Wisp2Scheduler.this;
        }
    }

    /**
     * try steal one task from the most busy carrier's queue
     *
     * @param n max retry times
     * @param r random seed
     */
    private Runnable trySteal(final int n, int r) {
        Carrier busyCarrier = null;
        for (int i = 0; i < n; i++) {
            final Carrier c = getCarrier(r + i);
            int ql = c.queueLength;
            if (ql > STEAL_HIGH_WATER_LEVEL) {
                Runnable task = c.pollTask(true);
                if (task != null) {
                    return task;
                }
            }
            if (busyCarrier == null || ql > busyCarrier.queueLength) {
                busyCarrier = c;
            }
        }
        // If busyCarrier's head is disable steal, we also miss the
        // chance of steal from second busy carrier..
        // For disable steal is uncommon path, current implementation is good enough..
        return busyCarrier == null ? null : busyCarrier.pollTask(true);
    }

    /**
     * Find an idle work, and push task to it's work queue
     *
     * @param n            retry times
     * @param ignoreIfBusy ignore this push operation if all carrier if busy
     * @param needEnqueue  also do enqueue
     * @param command      the task
     */
    private void findIdleAndWakeup(final int n, boolean ignoreIfBusy,
                                   boolean needEnqueue, StealAwareRunnable command) {
        int r = (seed = nextRandom(seed));
        Carrier idleCarrier = null;
        for (int i = 0; i < n; i++) {
            final Carrier c = getCarrier(r + i);
            if (c.idle()) {
                if (needEnqueue) {
                    c.pushAndSignal(command);
                } else {
                    c.signal();
                }
                return;
            } else if (idleCarrier == null || c.queueLength < idleCarrier.queueLength) {
                idleCarrier = c;
            }
        }
        if (!ignoreIfBusy && idleCarrier != null) {
            if (needEnqueue) {
                idleCarrier.pushAndSignal(command);
            } else {
                idleCarrier.signal();
            }
        }
    }

    private Carrier getCarrier(int r) {
        return carriers[(r & IDX_MASK) % PARALLEL];
    }

    /**
     * cast thread to carrier
     *
     * @param detachedAsNull treat detached carrier as not carrier thread if detachedAsNull is true.
     * @return null means thread is not considered as a carrier
     */
    private Carrier castToCarrier(Thread thread, boolean detachedAsNull) {
        if (thread == null) {
            return null;
        }
        Wisp2Engine engine = (Wisp2Engine) JLA.getWispEngine(thread);
        Carrier carrier = engine.carrier;
        if (carrier == null || carrier.theScheduler() != this) {
            return null;
        } else {
            return (detachedAsNull && carrier.detached) ? null : carrier;
        }
    }

    void addTimer(TimeOut timeOut, Thread current) {
        Carrier carrier = castToCarrier(current, true);
        if (carrier != null) {
            carrier.timerManager.addTimer(timeOut);
        } else {
            findIdleAndWakeup(1, false, true,
                    new StealAwareRunnable() {
                        @Override
                        public void run() {
                            Carrier carrier = castToCarrier(JLA.currentThread0(), true);
                            assert carrier != null;
                            carrier.timerManager.addTimer(timeOut);
                        }
                    });
        }
    }

    void cancelTimer(TimeOut timeOut, Thread current) {
        Carrier carrier = castToCarrier(current, true);
        if (carrier != null) {
            carrier.timerManager.cancelTimer(timeOut);
        }
    }

    /**
     * Run the command on the specified thread.
     * Used to implement Thread affinity for scheduler.
     * When execute with detached carrier thread, we try to execute this task by
     * other carriers, if this step failed command would be marked as can't be stolen,
     * then we push this command to detached carrier.
     *
     * @param command the code
     * @param thread  target thread
     */
    void executeWithCarrierThread(StealAwareRunnable command, Thread thread) {
        final Carrier carrier = castToCarrier(thread, false);
        if (carrier == null || (carrier.detached && command.isStealEnable())) {
            // detached carrier try to execute from global scheduler at first
            execute(command);
        } else if (!carrier.pushAndSignal(command) &&
                HELP_STEAL_RETRY > 0 && command.isStealEnable()) {
            findIdleAndWakeup(HELP_STEAL_RETRY, true, false, null);
        }
    }

    /**
     * Executes the given command at some time in the future.
     *
     * @param command the runnable task
     * @throws NullPointerException if command is null
     */
    public void execute(StealAwareRunnable command) {
        findIdleAndWakeup(PUSH_RETRY, false, true, command);
    }

    /**
     * Detach carrier and create a new carrier to replace it.
     */
    void handOffCarrierThread(Thread thread) {
        Carrier carrier = castToCarrier(thread, true);
        if (carrier != null && !carrier.detached) {
            carrier.detached = true;
            carrier.pushAndSignal(new StealAwareRunnable() {
                @Override
                public void run() {
                }
            }); // ensure `detached` visibility
            carrier.thread.setName(carrier.thread.getName() + " (HandOff)");
            Carrier[] cs = Arrays.copyOf(this.carriers, carriers.length);
            for (int i = 0; i < PARALLEL; i++) {
                if (cs[i] == carrier) {
                    cs[i] = new Carrier();
                    // tasks blocked on detached carrier may not be scheduled in time
                    // because it's in long-time syscall, so we try our best to delegate
                    // all context to the new carrier
                    cs[i].copyContextFromDetachedCarrier(carrier);
                    cs[i].thread.start();
                    break;
                }
            }
            carriers = cs;
            Wisp2Engine engine = (Wisp2Engine) JLA.getWispEngine(thread);
        }
    }

    private static void doExec(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private int seed = (int) System.nanoTime();

    private static int nextRandom(int r) {
        r ^= r << 13;
        r ^= r >>> 17;
        return r ^ (r << 5);
    }

    private static final AtomicIntegerFieldUpdater<Carrier> LENGTH_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(Carrier.class, "queueLength");
    private static final UnsafeAccess UA = SharedSecrets.getUnsafeAccess();
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
}
