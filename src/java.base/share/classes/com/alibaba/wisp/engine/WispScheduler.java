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
class WispScheduler {

    private static final SchedulingPolicy SCHEDULING_POLICY = WispConfiguration.SCHEDULING_POLICY;

    private final static int IDX_MASK = 0xffff; // ensure positive
    private final static int STEAL_HIGH_WATER_LEVEL = 4;

    // instance const
    private int PARALLEL;
    private int STEAL_RETRY;
    private int PUSH_RETRY;
    private int HELP_STEAL_RETRY;
    private final boolean IS_ROOT_CARRIER;

    // carriers could be changed by handOff(),
    // add volatile to avoiding carriers's elements
    // to be allocated in register.
    // we could not add volatile volatile modifier
    // to array elements, so make the array volatile.
    private volatile Carrier[] carriers;
    private final ThreadFactory threadFactory;
    private final WispGroup group;
    private int sharedSeed = randomSeed();

    WispScheduler(int parallelism, ThreadFactory threadFactory, WispGroup group) {
        this(parallelism, Math.max(1, parallelism / 2), parallelism,
                Math.max(1, parallelism / 4), threadFactory, group, false);
    }

    WispScheduler(int parallelism, int stealRetry, int pushRetry, int helpStealRetry,
                  ThreadFactory threadFactory, WispGroup group, boolean isRootCarrier) {
        assert parallelism > 0;
        PARALLEL = parallelism;
        STEAL_RETRY = stealRetry;
        PUSH_RETRY = pushRetry;
        HELP_STEAL_RETRY = helpStealRetry;
        IS_ROOT_CARRIER = isRootCarrier;
        this.group = group;
        this.threadFactory = threadFactory;
        carriers = new Carrier[PARALLEL];
        for (int i = parallelism - 1; i >= 0; i--) {
            carriers[i] = new Carrier();
            carriers[i].next = i == PARALLEL - 1 ? null : carriers[i + 1];
        }
        carriers[PARALLEL - 1].next = carriers[0];
        if (!isRootCarrier) {
            // root carrier threads are started in startWispDaemons()
            startCarrierThreads();
        }
    }

    void startCarrierThreads() {
        for (Carrier carrier : carriers) {
            carrier.thread.start();
        }
    }

    private int generateRandom() {
        return sharedSeed = nextRandom(sharedSeed);
    }

    class Carrier implements Runnable {
        ConcurrentLinkedQueue<StealAwareRunnable> taskQueue;
        private final TimeOut.TimerManager timerManager;
        private final Thread thread;
        volatile boolean detached = false;
        private Carrier next;

        private final static int QL_PROCESSING_TIMER = -1; // idle than ql=0, but not really idle
        private final static int QL_POLLING = -102;
        private final static int QL_IDLE = -202;

        volatile int queueLength;

        Carrier() {
            thread = threadFactory.newThread(this);
            WispEngine.carrierThreads.add(thread);
            taskQueue = new ConcurrentLinkedQueue<>();
            timerManager = new TimeOut.TimerManager();
            queueLength = 0;
        }

        void processTimer() {
            timerManager.processTimeoutEventsAndGetWaitDeadline(System.nanoTime());
        }

        @Override
        public void run() {
            try {
                WispEngine engine = WispEngine.current();
                engine.group = group;
                engine.carrier = this;
                group.carrierEngines.add(engine);
                engine.registerPerfCounter();
                runCarrier(engine);
            } finally {
                WispEngine.carrierThreads.remove(thread);
            }
        }

        private void runCarrier(final WispEngine engine) {
            int r = randomSeed();
            Runnable task;
            while (true) {
                while ((task = pollTask(false)) != null) {
                    doExec(task);
                }

                if (detached) {
                    if (engine.runningTaskCount == 0) {
                        return;
                    }
                } else if ((task = SCHEDULING_POLICY.steal(this, r = nextRandom(r))) != null) {
                    doExec(task);
                    continue; // process local queue
                }
                doParkOrPolling();
            }
        }

        private void doParkOrPolling() {
            int st = QL_PROCESSING_TIMER;
            if (queueLength != 0 || !LENGTH_UPDATER.compareAndSet(this, 0, st)) {
                return;
            }
            final long now = System.nanoTime();
            final long deadline = timerManager.processTimeoutEventsAndGetWaitDeadline(now);
            assert deadline != 0;
            if (queueLength == st) {
                int update = WispConfiguration.CARRIER_AS_POLLER && IS_ROOT_CARRIER &&
                        WispEventPump.INSTANCE.tryAcquire(this) ?
                        QL_POLLING : QL_IDLE;
                if (LENGTH_UPDATER.compareAndSet(this, st, update)) {
                    st = update;
                    if (taskQueue.peek() == null) {
                        if (st == QL_IDLE) {
                            UA.park0(false, deadline < 0 ? 0 : deadline - now);
                        } else { // st == QL_POLLING
                            doPolling(deadline, now);
                        }
                    }
                }
                if (update == QL_POLLING) {
                    WispEventPump.INSTANCE.release(this);
                }
            }
            LENGTH_UPDATER.addAndGet(this, -st);
        }

        private void doPolling(final long deadline, long now) {
            while ((deadline < 0 || deadline > now) &&
                    !WispEventPump.INSTANCE.pollAndDispatchEvents(
                            deadline < 0 ? -1 : TimeOut.nanos2Millis(deadline - now)) &&
                    queueLength == QL_POLLING) { // if wakened by event, we can still waiting..
                now = deadline < 0 ? now : System.nanoTime();
            }
        }

        /**
         * @return if it is idle
         */
        private boolean doSignalIfNecessary(int len) {
            Thread current = JLA.currentThread0();
            if (thread != current) {
                if (len == QL_IDLE) {
                    UA.unpark0(this.thread);
                } else if (len == QL_POLLING) {
                    WispEventPump.INSTANCE.wakeup();
                }
            }
            return len < 0;
        }

        boolean idleOrPolling() {
            return queueLength == QL_IDLE || queueLength == QL_POLLING;
        }

        boolean isProcessingTimer() {
            return queueLength == QL_PROCESSING_TIMER;
        }

        Runnable pollTask(boolean isSteal) {
            StealAwareRunnable task = taskQueue.poll();
            if (task != null) {
                LENGTH_UPDATER.decrementAndGet(this);
                if (isSteal && !task.isStealEnable()) {
                    // disable steal is a very uncommon case,
                    // The overhead of re-enqueue is acceptable
                    // use pushAndSignal rather than offer,
                    // let the carrier execute the task as soon as possible.
                    pushAndSignal(task);
                    return null;
                }
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

        void copyContextFromDetachedCarrier(Carrier detachedCarrier) {
            // copy timers
            timerManager.copyTimer(detachedCarrier.timerManager.queue);
            // drain wispTasks
            StealAwareRunnable task;
            while ((task = detachedCarrier.taskQueue.poll()) != null) {
                pushAndSignal(task);
            }
        }

        WispScheduler theScheduler() {
            return WispScheduler.this;
        }
    }

    /**
     * try steal one task from the most busy carrier's queue
     *
     * @param r random seed
     */
    private Runnable trySteal(int r) {
        Carrier busyCarrier = null;
        for (int i = 0; i < STEAL_RETRY; i++) {
            final Carrier c = getCarrier(r + i);
            int ql = c.queueLength;
            if (ql >= STEAL_HIGH_WATER_LEVEL) {
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
     * Find an idle carrier, and push task to it's work queue
     *
     * @param n       retry times
     * @param command the task
     * @param force   push even all carriers are busy
     * @return success push to a idle carrier
     */
    private boolean tryPush(int n, StealAwareRunnable command, boolean force) {
        assert n > 0;
        int r = generateRandom();
        Carrier idleCarrier = null;
        int idleQl = Integer.MAX_VALUE;

        Carrier c = getCarrier(r);
        for (int i = 0; i < n; i++, c = c.next) {
            if (c.idleOrPolling()) {
                if (command != null) {
                    c.pushAndSignal(command);
                } else {
                    c.signal();
                }
                return true;
            }
            int ql = c.queueLength;
            if (ql < idleQl) {
                idleCarrier = c;
                idleQl = ql;
            }
        }
        if (force) {
            assert idleCarrier != null && command != null;
            idleCarrier.pushAndSignal(command);
            return true;
        }
        return false;
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
        Carrier carrier = JLA.getWispEngine(thread).carrier;
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
            tryPush(1, new StealAwareRunnable() {
                @Override
                public void run() {
                    //Adding timer to detached carrier is ok since we rely on 
                    //interrupt to wakeup all wispTasks in shutdown
                    Carrier carrier = castToCarrier(JLA.currentThread0(), false);
                    assert carrier != null;
                    carrier.timerManager.addTimer(timeOut);
                }
            }, true);
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
        boolean stealEnable = command.isStealEnable();
        if (carrier == null || carrier.detached && stealEnable) {
            // detached carrier try to execute from global scheduler at first
            execute(command);
        } else {
            SCHEDULING_POLICY.enqueue(carrier, stealEnable, command);
        }
    }

    enum SchedulingPolicy {
        PULL {
            // always enqueue to the bounded carrier, but carriers will steal tasks from each other.
            @Override
            void enqueue(Carrier carrier, boolean stealEnable, StealAwareRunnable command) {
                WispScheduler scheduler = carrier.theScheduler();
                if (!carrier.pushAndSignal(command) && stealEnable && scheduler.HELP_STEAL_RETRY > 0) {
                    scheduler.signalIdleCarrierToHelpSteal();
                }
            }

            @Override
            Runnable steal(Carrier carrier, int r) {
                return carrier.theScheduler().trySteal(r);
            }
        },
        PUSH {
            // never try to pull other carrier's queues, but choose an idle carrier when we're enqueueing
            @Override
            void enqueue(Carrier carrier, boolean stealEnable, StealAwareRunnable command) {
                WispScheduler scheduler = carrier.theScheduler();
                if (stealEnable
                        && !(carrier.idleOrPolling() || carrier.isProcessingTimer())
                        && scheduler.STEAL_RETRY > 0
                        && scheduler.tryPush(scheduler.STEAL_RETRY, command, false)) {
                    return;
                }
                carrier.pushAndSignal(command);
            }

            @Override
            Runnable steal(Carrier carrier, int r) {
                return null;
            }
        };

        abstract void enqueue(Carrier carrier, boolean stealEnable, StealAwareRunnable command);

        abstract Runnable steal(Carrier carrier, int r);
    }

    private void signalIdleCarrierToHelpSteal() {
        tryPush(HELP_STEAL_RETRY, null, false);
    }

    /**
     * Executes the given command at some time in the future.
     *
     * @param command the runnable task
     * @throws NullPointerException if command is null
     */
    public void execute(StealAwareRunnable command) {
        tryPush(PUSH_RETRY, command, true);
    }

    /**
     * Detach carrier and create a new carrier to replace it.
     * This function should only be called by Wisp-Sysmon
     */
    void handOffCarrierThread(Thread thread) {
        assert WispSysmon.WISP_SYSMON_NAME.equals(Thread.currentThread().getName());
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
            Carrier last = cs[PARALLEL - 1];
            for (int i = 0; i < PARALLEL; i++) {
                if (cs[i] == carrier) {
                    cs[i] = new Carrier();
                    // tasks blocked on detached carrier may not be scheduled in time
                    // because it's in long-time syscall, so we try our best to delegate
                    // all context to the new carrier
                    cs[i].copyContextFromDetachedCarrier(carrier);
                    cs[i].next = carrier.next;
                    last.next = cs[i];
                    cs[i].thread.start();
                    break;
                }
                last = cs[i];
            }
            carriers = cs;
            JLA.getWispEngine(thread).deRegisterPerfCounter();
        }
    }

    /**
     * Check if current processor number exceeds carriers.length, if so we add new carriers
     * to this scheduler.
     * This function should only be called by Wisp-Sysmon
     */
    void checkAndGrowCarriers(int availableProcessors) {
        assert WispSysmon.WISP_SYSMON_NAME.equals(Thread.currentThread().getName());
        if (availableProcessors <= carriers.length) {
            return;
        }
        double growFactor = (double) availableProcessors / (double) carriers.length;
        Carrier[] cs = Arrays.copyOf(this.carriers, availableProcessors);
        for (int i = availableProcessors - 1; i >= carriers.length; i--) {
            if (cs[i] == null) {
                cs[i] = new Carrier();
                cs[i].next = i == availableProcessors - 1 ? cs[0] : cs[i + 1];
            }
        }
        cs[carriers.length - 1].next = cs[carriers.length];
        for (int i = carriers.length; i < availableProcessors; i++) {
            cs[i].thread.start();
        }
        int originLength = carriers.length;
        carriers = cs;
        adjustParameters(originLength, growFactor);
    }

    private void adjustParameters(int originLength, double growFactor) {
        PARALLEL = Integer.min((int) Math.round((double) originLength * growFactor),
                carriers.length);
        PUSH_RETRY = ((int) (PARALLEL * growFactor));
        STEAL_RETRY = ((int) (STEAL_RETRY * growFactor));
        HELP_STEAL_RETRY = ((int) (HELP_STEAL_RETRY * growFactor));
    }

    private static void doExec(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static int nextRandom(int r) {
        r ^= r << 13;
        r ^= r >>> 17;
        return r ^ (r << 5);
    }

    private static int randomSeed() {
        int r = 0;
        while (r == 0) {
            r = (int) System.nanoTime();
        }
        return r;
    }

    private static final AtomicIntegerFieldUpdater<Carrier> LENGTH_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(Carrier.class, "queueLength");
    private static final UnsafeAccess UA = SharedSecrets.getUnsafeAccess();
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
}
