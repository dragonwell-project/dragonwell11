package com.alibaba.wisp.engine;

import jdk.internal.misc.SharedSecrets;
import sun.nio.ch.EpollAccess;
import sun.nio.ch.Net;
import sun.nio.ch.SelChImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;



class WispEventPump {
    private static final int LOW_FD_BOUND = 1024 * 10;
    private static final int MAX_EVENTS_TO_POLL = 512;
    private static final EpollAccess EA;

    private final int epfd;
    private final int pipe0;
    private final int pipe1;
    private final long epollArray;

    static {
        sun.nio.ch.IOUtil.load();
        EA = SharedSecrets.getEpollAccess();
    }

    private WispEventPump() {
        try {
            epfd = EA.epollCreate();
            int[] a = new int[2];
            EA.socketpair(a);
            pipe0 = a[0];
            pipe1 = a[1];
            if (EA.epollCtl(epfd, EpollAccess.EPOLL_CTL_ADD, pipe0, Net.POLLIN) != 0) {
                throw new IOException("epoll_ctl fail");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        epollArray = EA.allocatePollArray(MAX_EVENTS_TO_POLL);
    }

    enum Pool {
        INSTANCE;
        private final int mask;
        private final WispEventPump[] pumps;

        Pool() {
            int n = Math.max(1, WispConfiguration.WORKER_COUNT / WispConfiguration.POLLER_SHARDING_SIZE);
            n = (n & (n - 1)) == 0 ? n : Integer.highestOneBit(n) * 2; // next power of 2
            mask = n - 1;
            pumps = new WispEventPump[n];
            for (int i = 0; i < pumps.length; i++) {
                pumps[i] = new WispEventPump();
            }
        }

        void startPollerThreads() {
            int i = 0;
            for (WispEventPump pump : pumps) {
                Thread t = new Thread(WispEngine.daemonThreadGroup, new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            pump.pollAndDispatchEvents(-1);
                        }
                    }
                }, "Wisp-Poller-" + i++);
                t.setDaemon(true);
                t.start();
            }
        }

        private static int hash(int x) {
            // implementation of Knuth multiplicative algorithm.
            return x * (int) 2654435761L;
        }

        void registerEvent(WispTask task, SelectableChannel ch, int event) throws IOException {
            if (ch != null && ch.isOpen()) {
                int fd = ((SelChImpl) ch).getFDVal();
                pumps[hash(fd) & mask].registerEvent(task, fd, event);
            }
        }

        int epollWaitForWisp(int epfd, long pollArray, int arraySize, long timeout, AtomicReference<Object> status,
                             final Object INTERRUPTED) throws IOException {
            return pumps[hash(epfd) & mask].epollWaitForWisp(epfd, pollArray, arraySize, timeout, status, INTERRUPTED);
        }

        void interruptEpoll(AtomicReference<Object> status, Object INTERRUPTED, int interruptFd) {
            WispEventPump.interruptEpoll(status, INTERRUPTED, interruptFd);
        }

        WispEventPump getPump(int ord) {
            return pumps[ord & mask];
        }
    }

    /**
     * fd2ReadTask handles all incoming io events like reading and accepting
     */
    private WispTask[] fd2ReadTaskLow = new WispTask[LOW_FD_BOUND];
    private ConcurrentHashMap<Integer, WispTask> fd2ReadTaskHigh = new ConcurrentHashMap<>();

    /**
     * fd2WriteTask handles all outing io events like connecting and writing
     */
    private WispTask[] fd2WriteTaskLow = new WispTask[LOW_FD_BOUND];
    private ConcurrentHashMap<Integer, WispTask> fd2WriteTaskHigh = new ConcurrentHashMap<>();

    /**
     * whether event is a reading event or an accepting event
     */
    private boolean isReadEvent(int events) throws IllegalArgumentException {
        int event = (events & (Net.POLLCONN | Net.POLLIN | Net.POLLOUT));
        assert Integer.bitCount(event) == 1;
        return (events & Net.POLLIN) != 0;
    }

    private WispTask[] getFd2TaskLow(int events) {
        return isReadEvent(events) ? fd2ReadTaskLow : fd2WriteTaskLow;
    }

    private ConcurrentHashMap<Integer, WispTask> getFd2TaskHigh(int events) {
        return isReadEvent(events) ? fd2ReadTaskHigh : fd2WriteTaskHigh;
    }

    private boolean sanityCheck(int fd, WispTask newTask, int events) {
        WispTask oldTask = fd < LOW_FD_BOUND ? getFd2TaskLow(events)[fd] : getFd2TaskHigh(events).get(fd);
        // If timeout happened, when oldTask finished,
        // the oldTask.ch would be nullified(we didn't get chance to remove it)
        return (oldTask == null || oldTask == newTask || oldTask.ch == null
                || ((SelChImpl) oldTask.ch).getFDVal() != fd);
    }

    /**
     * All events are guaranteed to be interested in only one direction since
     * all registrations are from WispSocket
     */
    private void recordTaskByFD(int fd, WispTask task, int events) {
        assert sanityCheck(fd, task, events);
        if (fd < LOW_FD_BOUND) {
            getFd2TaskLow(events)[fd] = task;
        } else {
            getFd2TaskHigh(events).put(fd, task);
        }
    }

    private WispTask removeTaskByFD(int fd, int events) {
        WispTask task;
        if (fd < LOW_FD_BOUND) {
            WispTask[] fd2TaskLow = getFd2TaskLow(events);
            task = fd2TaskLow[fd];
            fd2TaskLow[fd] = null;
        } else {
            task = getFd2TaskHigh(events).remove(fd);
        }
        return task;
    }

    private void registerEvent(WispTask task, int fd, int event) throws IOException {
        int ev = 0;
        // Translates an interest operation set into a native poll event set
        if ((event & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0) ev |= Net.POLLIN;
        if ((event & SelectionKey.OP_WRITE) != 0) ev |= Net.POLLOUT;
        if ((event & SelectionKey.OP_CONNECT) != 0) ev |= Net.POLLCONN;
        // When the socket is closed, the poll event will be triggered
        ev |= Net.POLLHUP;
        // specify the EPOLLONESHOT flag, to tell epoll to disable the associated
        // file descriptor after the receipt of an event with epoll_wait
        ev |= EpollAccess.EPOLLONESHOT;

        recordTaskByFD(fd, task, ev);
        task.setRegisterEventTime();
        // we can do it multi-thread, because epoll is protected by spin lock in kernel
        // When the EPOLLONESHOT flag is specified, it is the caller's responsibility to
        // rearm the file descriptor using epoll_ctl with EPOLL_CTL_MOD
        int res = EA.epollCtl(epfd, EpollAccess.EPOLL_CTL_MOD, fd, ev); // rearm
        if (res != 0 && !(res == EpollAccess.ENOENT && (res = EA.epollCtl(epfd, EpollAccess.EPOLL_CTL_ADD, fd, ev)) == 0)) {
            removeTaskByFD(fd, ev);
            task.resetRegisterEventTime();
            throw new IOException("epoll_ctl " + res);
        }
    }

    /**
     * API for coroutine do epoll_wait
     *
     * @param epfd        epoll fd
     * @param pollArray   epoll array address
     * @param arraySize   epoll array size
     * @param timeout     timeout ms
     * @param status      interrupt status;
     * @param INTERRUPTED const indicate for interrupted
     * @return selected event num
     */
    private int epollWaitForWisp(int epfd, long pollArray, int arraySize, long timeout,
                                 AtomicReference<Object> status, final Object INTERRUPTED) throws IOException {
        assert pollArray != 0;
        WispTask me = WispCarrier.current().current;
        if (!WispEngine.runningAsCoroutine(me.getThreadWrapper())) {
            return EA.epollWait(epfd, pollArray, arraySize, (int) timeout);
        }
        if (WispConfiguration.MONOLITHIC_POLL) {
            if (timeout == 0) {
                // return 0 for selectNow(), prevent calling epoll_wait in non-poller thread
                // and the application will retry with timeout
                return 0;
            }
        } else {
            int updated = EA.epollWait(epfd, pollArray, arraySize, 0);
            if (timeout == 0 || updated > 0) {
                return updated;
            }
        }
        if (WispConfiguration.USE_DIRECT_SELECTOR_WAKEUP &&
                (status.get() == INTERRUPTED || !(status.get() == null && status.compareAndSet(null, me)))) {
            assert status.get() == INTERRUPTED;
            return 0; // already epoll_wait(0), no retry needed.
        }

        if (WispConfiguration.MONOLITHIC_POLL) {
            assert timeout != 0;
            me.epollArraySize = arraySize;
            me.setEpollEventNum(0);
            me.setEpollArray(pollArray);
        }

        if (timeout != 0) {
            registerEvent(me, epfd, SelectionKey.OP_READ);
            WispTask.jdkPark(TimeUnit.MILLISECONDS.toNanos(timeout));
        }

        if (WispConfiguration.USE_DIRECT_SELECTOR_WAKEUP &&
                !(status.get() == me && status.compareAndSet(me, null))) {
            assert status.get() == INTERRUPTED;
        }

        if (WispConfiguration.MONOLITHIC_POLL) {
            // already polled by poller, see doMonolithicPoll()
            me.setEpollArray(0);
            return me.getEpollEventNum();
        } else {
            return EA.epollWait(epfd, pollArray, arraySize, 0);
        }
    }

    private void doMonolithicPoll(int fd, WispTask task, long epollArray) throws IOException {
        assert WispConfiguration.MONOLITHIC_POLL;
        task.setEpollEventNum(EA.epollWait(fd, epollArray, task.epollArraySize, 0));
    }

    private static void interruptEpoll(AtomicReference<Object> status, Object INTERRUPTED, int interruptFd) {
        assert WispConfiguration.USE_DIRECT_SELECTOR_WAKEUP;
        while (true) {
            final Object st = status.get();
            if (st == INTERRUPTED || st == null && status.compareAndSet(null, INTERRUPTED)) {
                if (!WispConfiguration.ALL_THREAD_AS_WISP) {
                    try {
                        EA.interrupt(interruptFd);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                break;
            } else if (st != null) { // waiting
                assert st instanceof WispTask;
                if (status.compareAndSet(st, INTERRUPTED)) {
                    ((WispTask) st).jdkUnpark();
                    break;
                }
            }
        }
    }

    private volatile int wakeupCount;

    boolean pollAndDispatchEvents(long timeout) {
        boolean wakened = false;
        try {
            int n = EA.epollWait(epfd, epollArray, MAX_EVENTS_TO_POLL, (int) timeout);
            while (n-- > 0) {
                long eventAddress = EA.getEvent(epollArray, n);
                int fd = EA.getDescriptor(eventAddress);
                if (fd == pipe0) {
                    wakened = true;
                    // Conservative strategy, wakeup() can never lost
                    if (WAKEUP_UPDATER.decrementAndGet(this) == 0) {
                        EA.drain(pipe0);
                    }
                    continue;
                }
                int events = EA.getEvents(eventAddress);
                if ((events & Net.POLLIN) != 0) {
                    processEvent(fd, true);
                }
                if ((events & Net.POLLCONN) != 0 || (events & Net.POLLOUT) != 0) {
                    processEvent(fd, false);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return wakened;
    }


    private void processEvent(int fd, boolean isRead) throws IOException {
        WispTask task = removeTaskByFD(fd, isRead ? Net.POLLIN : Net.POLLOUT);
        if (task != null) {
            long epollArray = task.getEpollArray();
            if (isRead && epollArray != 0) {
                doMonolithicPoll(fd, task, epollArray);
            } else {
                task.countWaitSocketIOTime();
            }
            task.jdkUnpark();
        }
    }

    void wakeup() {
        if (WAKEUP_UPDATER.getAndIncrement(this) == 0) {
            try {
                EA.interrupt(pipe1);
            } catch (IOException x) {
                throw new UncheckedIOException(x);
            }
        }
    }

    volatile WispScheduler.Worker owner;

    boolean tryAcquire(WispScheduler.Worker worker) {
        assert WispConfiguration.CARRIER_AS_POLLER;
        return owner == null && OWNER_UPDATER.compareAndSet(this, null, worker);
    }

    void release(WispScheduler.Worker worker) {
        assert owner == worker;
        OWNER_UPDATER.lazySet(this, null);
    }

    private final static AtomicReferenceFieldUpdater<WispEventPump, WispScheduler.Worker> OWNER_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(WispEventPump.class, WispScheduler.Worker.class, "owner");
    private final static AtomicIntegerFieldUpdater<WispEventPump> WAKEUP_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(WispEventPump.class, "wakeupCount");
}
