package com.alibaba.wisp.engine;

import jdk.internal.misc.SharedSecrets;
import sun.nio.ch.EPollSelectorProvider;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-thread self-scheduled implementation of WispEngine.
 * These tasks are bound to Engine after they are created.
 * We could create tasks in different WispEngine to make maximum use of multiprocessing.
 * <p>
 * Here's how {@link java.net.Socket} and {@link WispSelector} cooperation with Java's NIO.
 * +----------------------------------------------------------------------------------------------------+
 * |                                                                                                    |
 * | WispEngine.current()               +---------------+  After nioSelector.select(), dispatch events  |
 * |                                 +->|  nioSelector  | ------------------------+  to wispSelectors,  |
 * |                                 |  +---------------+ <---------------------+ |  and wake up task   |
 * |                                 |                                          | |  who block on       |
 * |                                 |      ch.register(wispSelector, event)    | |  wispSelectors.     |
 * |                                 |      will proxy the request to engine's  | |                     |
 * |                                 |      nioSelector.                        | |                     |
 * |    +----------+  block on       |                  +----------+  block on  | v                     |
 * |    | wispTask | --------- socket                   | wispTask | ---------- wispSelector            |
 * |    +----------+                                    +----------+                                    |
 * |     this task use bio process one connection      this task use NIO api handle                     |
 * |                                                   lots of connections..                            |
 * |    +----------+  block on                         +----------+                                     |
 * |    | wispTask | --------- socket                  | wispTask |             wispSelector            |
 * |    +----------+                                   +----------+                                     |
 * |                                                                                                    |
 * |    +----------+                                   +----------+                                     |
 * |    | wispTask |           socket                  | wispTask |                                     |
 * |    +----------+                                   +----------+                                     |
 * |                                                                                                    |
 * |    Task may not block on socket or wispSelector (may be park(), sleep etc..).                      |
 * |    And Socket may not related on wispTask (in connection pool etc..).                              |
 * |                                                                                                    |
 * +----------------------------------------------------------------------------------------------------+
 */
final class ScheduledWispEngine extends WispEngine {

    @Override
    protected void preloadClasses() throws Exception {
        ScheduledWispEngine engine = ((ScheduledWispEngine) WispEngine.current());
        engine.selector(true);
        WispTask.jdkPark(TimeUnit.MILLISECONDS.toNanos(1));

        engine.selector.close();
        engine.selector = null;
    }

    private Selector newSelector() {
        boolean isInCritical0 = isInCritical;
        isInCritical = true;
        try {
            SelectorProvider p = SelectorProvider.provider();
            if (p instanceof EPollSelectorProvider)
                return ((EPollSelectorProvider) p).openSelector0();
            else
                throw new IllegalStateException("unexpected provider");

        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            isInCritical = isInCritical0;
        }
    }

    private Selector selector;
    private AtomicBoolean wakened = new AtomicBoolean(true);
    private Queue<WispTask> pendingTaskQueue = new ArrayDeque<>();

    @Override
    public void execute(Runnable target) {
        dispatchTask(target, "executor task", null);
    }

    @Override
    protected void dispatchTask(Runnable target, String name, Thread t) {
        if (JLA.currentThread0() == thread) {
            runTaskInternal(target, name, t, current.ctxClassLoader);
        } else {


         /*
          I wanna create real task here, but coroutine's API
          push users create coroutine at target thread.
          So we create a fake coroutine, and let the engine
          create real coroutine while got this pseudo-coroutine
          in taskQueue.
          @see {@link #processWakeTask(WispTask)}
         */
            ClassLoader ctxClassLoader = current.ctxClassLoader;
            WispTask pseudo = new WispTask(this, null, false, false);
            long enqueueTime = getNanoTime();
            pseudo.command = () -> {
                if (runningTaskCount >= WispConfiguration.EXTERNAL_SUBMIT_THRESHOLD) {
                    pendingTaskQueue.offer(pseudo);
                    return false;
                } else {
                    WispEngine.current().countEnqueueTime(enqueueTime);
                    runTaskInternal(target, name, t, ctxClassLoader);
                    return true; // means real switch happened
                }
            };
            wakeupTask(pseudo);
        }
    }

    @Override
    WispTask getTaskFromCache() {
        return taskCache.isEmpty() ? null : taskCache.remove(taskCache.size() - 1);
    }

    @Override
    void returnTaskToCache(WispTask task) {
        taskCache.add(task);
    }

    /**
     * Put wakened task in this queue, and execute the task before this engine gets blocked on Selector
     */
    private Deque<WispTask> wakeupQueue = new ConcurrentLinkedDeque<>();
    private TimeOut.TimerManager timerManager = new TimeOut.TimerManager();
    private Iterator<SelectionKey> eventIterator;
    private List<WispTask> yieldTasks = new ArrayList<>();

    @Override
    protected boolean wakeupTask(WispTask task, boolean front) {
        WispEngine engine = WispEngine.current();
        boolean isInCritical0 = engine.isInCritical;
        engine.isInCritical = true;
        try {
            boolean needWakeup = task != null && !task.enqueued.get() &&
                    task.enqueued.compareAndSet(false, true);
            if (needWakeup) {
                if (front) {
                    wakeupQueue.addFirst(task);
                } else {
                    wakeupQueue.addLast(task);
                }
            } else {
                statistics.alreadyWakeup++;
            }
            task.updateEnqueueTime();
            this.wakeup();
            return needWakeup;
        } finally {
            engine.isInCritical = isInCritical0;
        }
    }

    @Override
    protected void yield() {
        if (runningTaskCount > 0 || !wakeupQueue.isEmpty()) {
            // for history reason, thread-coroutine is not
            // treated as one runningTask, engine.runningTaskCount == 0
            // means only thread-coroutine, keep the raw thread behavior
            if (selector == null || selector.keys().isEmpty()) {
                wakeupTask(current, false); // append to wakeup queue's tail
            } else {
                // try best to run IO blocked tasks so that we put yielded
                // tasks in separate queue which will be handled at end of doSelect
                yieldTasks.add(current);
            }
            schedule(); // and do schedule
        } else {
            SharedSecrets.getJavaLangAccess().yield0();
        }
    }

    @Override
    protected void doSchedule() {
        int rescheduleCnt = 0;

        while (true) {
            statistics.maxReschedule = Math.max(rescheduleCnt++, statistics.maxReschedule);
            WispTask task;

            if (eventIterator != null && eventIterator.hasNext()) {
                // 1. check completed IO
                task = ((WispTask) eventIterator.next().attachment());
                eventIterator.remove();
                if (yieldTo(task)) {
                    statistics.doIO++;
                    break;
                }
            } else if ((runningTaskCount < WispConfiguration.EXTERNAL_SUBMIT_THRESHOLD
                    && (task = pendingTaskQueue.poll()) != null)
                    || (task = wakeupQueue.poll()) != null) {
                // 2. check wakened task
                if (processWakeTask(task)) {
                    statistics.doWakeup++;
                    break;
                }
            } else {
                // 3. check timer queue
                TimeOut timeOut;
                if ((timeOut = timerManager.queue.peek()) != null &&
                        timeOut.deadlineNano <= System.nanoTime()) {
                    if (yieldTo(timerManager.queue.poll().task)) {
                        statistics.doTimer++;
                        counter.incrementTimeOutCount();
                        break;
                    }
                } else {
                    /*
                     * Another thread may call {@link #wakeup()} here, therefore
                     * if we don't check again, the last {@link #wakeupTask(WispTask)}  will get lost.
                     */
                    wakened.set(false);

                    // double check unpark queue
                    if ((task = wakeupQueue.poll()) != null) {
                        statistics.doubleCheckWakeup++;
                        wakened.lazySet(true);
                        if (processWakeTask(task)) {
                            statistics.doWakeup++;
                            break;
                        }
                    }
                    /*
                     * {@link #wakeupTask(WispTask)} here is ok:
                     * The {@link #wakeupTask(WispTask)} call will do a {@link Selector#wakeup() ,
                     * to ensure the pump returns immediately.
                     */
                    // nothing to do ? call select, and retry
                    try {
                        long ms = -1;
                        if (0 != timerManager.queue.size()) {
                            ms = TimeOut.nanos2Millis(timerManager.queue.peek().deadlineNano - System.nanoTime());
                        }
                        if (!yieldTasks.isEmpty()) {
                            ms = 0;
                        }
                        doSelect(ms);
                    } catch (IOException e) {
                        // pass and retry
                    }
                }
            }
        }
    }

    private boolean processWakeTask(WispTask task) {
        task.enqueued.lazySet(false);
        if (task.command != null) {
            return task.command.get();
        } else {
            countEnqueueTime(task.getEnqueueTime());
            task.resetEnqueueTime();
            return yieldTo(task);
        }
    }

    @Override
    protected void addTimer(long deadlineNano, boolean fromJvm) {
        TimeOut timeOut = new TimeOut(current, deadlineNano, fromJvm);
        current.timeOut = timeOut;
        timerManager.addTimer(timeOut);
    }

    @Override
    protected void cancelTimer() {
        if (current.timeOut != null) {
            current.timeOut.canceled = true;
            timerManager.cancelTimer(current.timeOut);
            current.timeOut = null;
        }
    }

    @Override
    protected void registerEvent(WispTask target, SelectableChannel ch, int events)
            throws IOException {

        if (ch == null || !ch.isOpen())
            return;

        boolean isInCritical0 = isInCritical;
        isInCritical = true;
        try {
            Selector sel = selector(false);
            if (sel == null) {
                if (events != 0) { // wispPoller is one shot, do not need clear
                    WispEventPump.INSTANCE.registerEvent(target, ch, events);
                }
                return;
            }

            int retry = 4;
            while (retry-- > 0) {
                try {
                    // detach old interest ch
                    if (target.ch != null && target.ch != ch) {
                        SelectionKey key = target.ch.keyFor(sel);
                        if (key != null && key.isValid() && key.interestOps() != 0 &&
                                key.attachment() == target) {
                            key.interestOps(0); // may produce CancelledKeyException
                        }
                    }

                    SelectionKey key = ch.keyFor(sel);
                    if (key != null) {
                        // nio put canceled key to a list,
                        // and process it next loop. so retry
                        if (!key.isValid()) {
                            statistics.retryCanceledKey++;
                            doSelect(0);
                            continue;
                        }
                        if (events == 0) {
                            if (key.attachment() == target) {
                                key.interestOps(0); // may produce CancelledKeyException
                            }
                        } else {
                            if (key.interestOps() != events) {
                                key.interestOps(events); // may produce CancelledKeyException
                            }
                            if (key.attachment() != target) {
                                key.attach(target);
                            }
                        }
                    } else {
                        ch.register(sel, events, target);
                    }
                    target.ch = ch;
                    return;
                } catch (CancelledKeyException e) {
                    statistics.retryCanceledKey++;
                    doSelect(0);
                } catch (ClosedChannelException e) {
                    // pass and retry
                }
            }
            throw new IOException();
        } finally {
            isInCritical = isInCritical0;
        }
    }


    // record if the NIO consume 100% CPU BUG happened
    private static final int REBUILD_THRESHOLD = 128;
    private int rebuildStatus = 0;

    // record idle rate
    private long selectTimestamp;
    private long cycleDuration;
    private int idlePerK = 1000;

    /**
     * Wake up the engine that is blocked on {@link Selector#select()}.
     * <p>
     * {@link Selector#wakeup()} is an expensive operation(fd write),
     * so use a status variable to reduce the cost.
     */
    private void wakeup() {
        statistics.wakeupCount++;
        WispEngine self = WispEngine.current(); // self wakeup this
        if (this != self) {
            if (!wakened.get() && wakened.compareAndSet(false, true)) {
                statistics.realWakeupCount++;
                boolean isInCritical0 = self.isInCritical;
                self.isInCritical = true;
                if (selector == null) {
                    // selector may became non-null
                    // and use select() in the target engine
                    // see WispEngine#selector()
                    SharedSecrets.getUnsafeAccess().unpark0(thread);
                } else {
                    selector.wakeup();
                }
                self.isInCritical = isInCritical0;
            }
        } else {
            statistics.innerEngineWakeup++;
        }
    }

    /**
     * 1. count running status (select is the only way to block current thread)
     * 2. handle NIO bug
     * 3. dispatch event to proxied Selector
     *
     * @param ms == 0   ->  selectNow()
     *           ms <  0   ->  select()
     *           ms >  0   ->  select(timeout)
     * @return selectedCount
     * @throws IOException
     */
    int doSelect(long ms) throws IOException {
        schedTick++;
        Selector sel = selector;
        statistics.eventLoops++;
        counter.incrementEventLoopCount();
        int selectCnt = -1;

        boolean isInCritical0 = isInCritical;
        isInCritical = true;

        try {
            if (wakened.get() || ms == 0) {
                statistics.nonBlockSelectCount++;
                if (sel != null) {
                    selectCnt = sel.selectNow();
                }
            } else {

                long startTs = System.nanoTime();

                long busyTime = selectTimestamp == 0 ? 0 : startTs - selectTimestamp;
                counter.incrementRunningTimeTotal(busyTime);

                boolean interrupted = current.isInterrupted();
                if (interrupted) { // avoid unexpected wakeup, see AbstractSelector.begin()
                    current.lazySetInterrupted(false);
                }
                // if a selector.wakeup() called before here,
                // selector.select(...)' will wake up immediately.
                if (sel != null) {
                    selectCnt = ms > 0 ? sel.select(ms) : sel.select();
                } else {
                    SharedSecrets.getUnsafeAccess().park0(false, ms < 0 ? 0 : TimeUnit.MILLISECONDS.toNanos(ms));
                }
                if (interrupted) {
                    current.lazySetInterrupted(true);
                }

                selectTimestamp = System.nanoTime();
                long idleTime = selectTimestamp - startTs;
                counter.incrementWaitTime(idleTime);
                long currentCycle = idleTime + busyTime;
                int currentIdle = currentCycle == 0 ? 500 :
                        (int) (1000 * idleTime / (currentCycle));

                int currentIdleRate = (int) ((idlePerK * cycleDuration + currentIdle * currentCycle) /
                        (currentCycle + cycleDuration));
                // weighted mean by cycle duration
                idlePerK = (int) exponentialSmoothed(idlePerK, currentIdleRate);
                cycleDuration = exponentialSmoothed(cycleDuration, currentCycle);


                if (selectCnt == 0 && idleTime <= 100000 /* 0.1ms */ && !wakened.get()) {
                    // In ajdk 8.4.8, While unparking a thread(which actually waking up the underlying selector
                    // used by WispEngine), an IOException might occur if the unparking is issued on a 'closed'
                    // selector(which has been made as 'closed' after rebuilding)
                    // To solve it, before calling rebuildSelector, set wakened as true. Then selector.wakeup
                    // won't be called (see wakeup)
                    if (rebuildStatus++ > REBUILD_THRESHOLD && wakened.compareAndSet(false, true)) {
                        rebuildSelector();
                        rebuildStatus = 0;
                    }
                } else {
                    rebuildStatus = 0;
                }
                // reduce the cost of following {@link #wakeup}
                wakened.lazySet(true);
            }

            if (sel == null) {
                return 0;
            }

            if (selectCnt != 0) {
                eventIterator = selector.selectedKeys().iterator();
                statistics.ioEventCount += selectCnt;
            } else {
                if (ms == 0) {
                    statistics.selectNothingActive++;
                } else {
                    statistics.selectNothingPassive++;
                }
            }

            for (WispTask task : yieldTasks) {
                wakeupTask(task, false);
            }
            yieldTasks.clear();

            return selectCnt;

        } finally {
            isInCritical = isInCritical0;
        }
    }


    Selector selector(boolean ensureCreate) {
        if (selector == null && (!WispConfiguration.GLOBAL_POLLER || ensureCreate)) {
            selector = newSelector();
            /*
             * thread A sees selector == null in WispEngine.wakeup()
             * thread B calls WispEngine.selector(true) and then selector.select() on newly created selector.
             * A unparks for thread B via unsafe, but unfortunately it will fail as thread B currently is
             * blocking on  selector.select()  (instead of parker)
             * So we do  selector.wakeup() to selector.select()  to prevent this problem.
             */
            selector.wakeup();
        }
        return selector;
    }

    @Override
    protected boolean isRunning() {
        return wakened.get();
    }

    @Override
    protected int getTaskQueueLength() {
        return wakeupQueue.size() + pendingTaskQueue.size() +
                (selector == null ? 0 : selector.selectedKeys().size());
    }


    /**
     * handle NIO consuming 100% CPU BUG
     * see https://issues.apache.org/jira/browse/DIRMINA-678
     */
    private void rebuildSelector() throws IOException {
        assert selector != null;
        final Selector oldSelector = selector;
        final Selector newSelector;

        try {
            newSelector = newSelector();
        } catch (Exception e) {
            throw new IOException(e);
        }

        // Register all channels to the new Selector.
        while (true) {
            try {
                for (SelectionKey key : oldSelector.keys()) {
                    Object a = key.attachment();
                    try {
                        if (!key.isValid() || key.channel().keyFor(newSelector) != null) {
                            continue;
                        }

                        int interestOps = key.interestOps();
                        key.cancel();
                        key.channel().register(newSelector, interestOps, a);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
            } catch (ConcurrentModificationException e) {
                // Probably due to concurrent modification of the key set.
                continue;
            }

            break;
        }

        selector = newSelector;
        statistics.selectorRebuild++;

        try {
            oldSelector.close();
        } catch (Throwable t) {
            // pass
        }
    }

    @Override
    protected void closeEngineSelector() {
        boolean isInCritical0 = isInCritical;
        if (selector != null) {
            try {
                isInCritical = true;
                selector.close();
            } catch (IOException e) {
                // ignore
            } finally {
                isInCritical = isInCritical0;
            }
        }
    }

    @Override
    public void shutdown() {
        hasBeenShutdown = true;
        deRegisterPerfCounter();
        if (WispEngine.current().current == threadTask) {
            doShutdown();
        } else {
            startShutdown();
        }
    }

    void doShutdown() {
        if (current == threadTask) {
            iterateTasksForShutdown();
            thread.getCoroutineSupport().drain();
            shutdownFuture.countDown();
        } else {
            UNSAFE.throwException(new ThreadDeath());
        }
    }


    private void startShutdown() {
        threadTask.enqueued.lazySet(false);
        // if threadTask has been enqueued, "switch to threadTask"
        // may happen before `hasBeenShutdown` has been seen.
        // ignore WispTask.enqueued flag, to ensure threadTask
        // could be scheduled after `hasBeenShutdown` has been seen

        // let threadTask launch killing process
        wakeupTask(threadTask, true);
    }

    private void iterateTasksForShutdown() {
        while (runningTaskCount != 0) {
            List<WispTask> runningTasks = getRunningTasks();
            for (WispTask task : runningTasks) {
                wakeupTask(task);
            }
            // ensure we could come back again and
            // check if all tasks has been exited
            wakeupTask(current);
            doSchedule();
        }
    }

    @Override
    protected void runTaskYieldEpilog() {
        if (hasBeenShutdown) {
            doShutdown();
        }
    }
}
