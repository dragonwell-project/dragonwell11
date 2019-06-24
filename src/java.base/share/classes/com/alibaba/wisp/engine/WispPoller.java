package com.alibaba.wisp.engine;

import jdk.internal.misc.SharedSecrets;
import sun.nio.ch.EpollAccess;
import sun.nio.ch.Net;
import sun.nio.ch.SelChImpl;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentHashMap;

public enum WispPoller {

    INSTANCE;

    private static final int LOW_FD_BOUND = 1024 * 10;
    private static final int MAX_EVENTS_TO_POLL = 512;

    private final EpollAccess EA;
    private final int epfd;
    private Thread thread;

    WispPoller() {
        if (WispConfiguration.GLOBAL_POLLER) {
            sun.nio.ch.IOUtil.load();
            EA = SharedSecrets.getEpollAccess();
            try {
                epfd = EA.epollCreate();
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
            thread = new Thread(WispEngine.daemonThreadGroup, this::eventLoop, "Wisp-Poller");
            thread.setDaemon(true);
            thread.start();
        } else {
            EA = null;
            epfd = 0;
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

    private void wakeupTaskByFD(int fd, int events) {
        if ((events & Net.POLLIN) != 0) {
            wakeupTask(removeTaskByFD(fd, Net.POLLIN));
        }
        if ((events & Net.POLLCONN) != 0 || (events & Net.POLLOUT) != 0) {
            wakeupTask(removeTaskByFD(fd, Net.POLLOUT));
        }
    }

    private void wakeupTask(WispTask task) {
        if (task == null) {
            return;
        }
        task.countWaitSocketIOTime();
        task.jdkUnpark();
    }

    void registerEvent(WispTask task, SelectableChannel ch, int event) throws IOException {
        if (ch == null || !ch.isOpen()) {
            return;
        }
        registerEvent(task, ((SelChImpl) ch).getFDVal(), event);
    }

    void registerEvent(WispTask task, int fd, int event) throws IOException {
        int ev = 0;
        // Translates an interest operation set into a native poll event set
        if ((event & SelectionKey.OP_READ) != 0) ev |= Net.POLLIN;
        if ((event & SelectionKey.OP_WRITE) != 0) ev |= Net.POLLOUT;
        if ((event & SelectionKey.OP_CONNECT) != 0) ev |= Net.POLLCONN;
        if ((event & SelectionKey.OP_ACCEPT) != 0) ev |= Net.POLLIN;
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

    private void eventLoop() {
        final long epollArray = EA.allocatePollArray(MAX_EVENTS_TO_POLL);
        while (true) {
            try {
                int n = EA.epollWait(epfd, epollArray, MAX_EVENTS_TO_POLL);
                while (n-- > 0) {
                    long eventAddress = EA.getEvent(epollArray, n);
                    int fd = EA.getDescriptor(eventAddress);
                    int event = EA.getEvents(eventAddress);
                    wakeupTaskByFD(fd, event);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

}
