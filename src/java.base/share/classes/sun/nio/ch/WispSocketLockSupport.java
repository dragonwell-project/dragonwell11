package sun.nio.ch;

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

import java.util.concurrent.locks.ReentrantLock;

/**
 * This class supports fd use across {@link WispTask} by adding read/write reentrantLocks for
 * {@link WispSocketImpl} {@link WispServerSocketImpl} and {@link WispUdpSocketImpl},
 * {@link WispTask}s will park once they encountered a contended socket.
 */
class WispSocketLockSupport {
    private static WispEngineAccess WEA = SharedSecrets.getWispEngineAccess();

    /** Lock held when reading and accepting
     */
    private final ReentrantLock readLock = WispEngine.enableSocketLock()? new ReentrantLock() : null;

    /** Lock held when writing and connecting
     */
    private final ReentrantLock writeLock = WispEngine.enableSocketLock()? new ReentrantLock() : null;

    /** Lock held when tracking current blocked WispTask and closing
     */
    private final ReentrantLock stateLock = WispEngine.enableSocketLock()? new ReentrantLock() : null;

    WispTask blockedReadWispTask  = null;
    WispTask blockedWriteWispTask = null;

    /**
     * This is not a  ReadWriteLock, they are separate locks here, readLock protect
     * reading to fd while writeLock protect writing to fd.
     */
    private void lockRead() {
        readLock.lock();
    }

    private void lockWrite() {
        writeLock.lock();
    }

    private void unLockRead() {
        readLock.unlock();
    }

    private void unLockWrite() {
        writeLock.unlock();
    }

    void beginRead() {
        if (!WispEngine.enableSocketLock()) {
            return;
        }
        lockRead();
        stateLock.lock();
        try {
            blockedReadWispTask = WEA.getCurrentTask();
        } finally {
            stateLock.unlock();
        }
    }

    void endRead() {
        if (!WispEngine.enableSocketLock()) {
            return;
        }

        stateLock.lock();
        try {
            blockedReadWispTask = null;
        } finally {
            stateLock.unlock();
        }
        unLockRead();
    }

    void beginWrite() {
        if (!WispEngine.enableSocketLock()) {
            return;
        }
        lockWrite();
        stateLock.lock();
        try {
            blockedWriteWispTask = WEA.getCurrentTask();
        } finally {
            stateLock.unlock();
        }
    }

    void endWrite() {
        if (!WispEngine.enableSocketLock()) {
            return;
        }
        stateLock.lock();
        try {
            blockedWriteWispTask = null;
        } finally {
            stateLock.unlock();
        }
        unLockWrite();
    }

    void unparkBlockedWispTask() {
        stateLock.lock();
        try {
            if (blockedReadWispTask != null) {
                WEA.unpark(blockedReadWispTask);
                blockedReadWispTask = null;
            }
            if (blockedWriteWispTask != null) {
                WEA.unpark(blockedWriteWispTask);
                blockedWriteWispTask = null;
            }
        } finally {
            stateLock.unlock();
        }
    }
}
