/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.dyn;

import jdk.internal.HotSpotIntrinsicCandidate;
import jdk.internal.misc.SharedSecrets;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Jvm entry of coroutine APIs.
 */
public class CoroutineSupport {
    /*
     Controls debugging and tracing, for maximum performance the actual if(DEBUG/TRACE) code needs to be commented out
     the inner of synchronized System.out.println() may result in coroutine switch
     DO NOT enable DEBUG, TRACE flag before we have a solution.
      */
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    private static final boolean CHECK_LOCK = true;
    private static final int SPIN_BACKOFF_LIMIT = 2 << 8;

    private static AtomicInteger idGen = new AtomicInteger();

    // The thread that this CoroutineSupport belongs to. There's only one CoroutineSupport per Thread
    private final Thread thread;
    // The initial coroutine of the Thread
    private final Coroutine threadCoroutine;

    // The currently executing coroutine
    private Coroutine currentCoroutine;

    private volatile Thread lockOwner = null; // also protect double link list of JavaThread->coroutine_list()
    private int lockRecursive; // volatile is not need

    private final int id;
    private boolean terminated = false;

    static {
        registerNatives();
    }

    /**
     * Allocates a new {@code CoroutineSupport} object.
     *
     * @param thread the Thread
     */
    public CoroutineSupport(Thread thread) {
        if (thread.getCoroutineSupport() != null) {
            throw new IllegalArgumentException("Cannot instantiate CoroutineThreadSupport for existing Thread");
        }
        id = idGen.incrementAndGet();
        this.thread = thread;
        threadCoroutine = new Coroutine(this, getThreadCoroutine());
        threadCoroutine.next = threadCoroutine;
        threadCoroutine.last = threadCoroutine;
        currentCoroutine = threadCoroutine;
    }

    /**
     * return the threadCoroutine
     *
     * @return threadCoroutine
     */
    public Coroutine threadCoroutine() {
        return threadCoroutine;
    }

    void addCoroutine(Coroutine coroutine, long stacksize) {
        assert currentCoroutine != null;
        lock();
        try {
            coroutine.data = createCoroutine(coroutine, stacksize);
            if (false && DEBUG) {
                System.out.println("add Coroutine " + coroutine + ", data" + coroutine.data);
            }

            // add the coroutine into the doubly linked ring
            coroutine.next = currentCoroutine.next;
            coroutine.last = currentCoroutine;
            currentCoroutine.next = coroutine;
            coroutine.next.last = coroutine;
        } finally {
            unlock();
        }
    }

    Thread getThread() {
        return thread;
    }

    /**
     * drain all alive coroutines.
     */
    public void drain() {
        if (Thread.currentThread() != thread) {
            throw new IllegalArgumentException("Cannot drain another threads CoroutineThreadSupport");
        }

        if (false && DEBUG) {
            System.out.println("draining");
        }
        lock();
        try {
            // drain all coroutines
            while (currentCoroutine.next != currentCoroutine) {
                symmetricExitInternal(currentCoroutine.next);
            }

            CoroutineBase coro;
            while ((coro = cleanupCoroutine()) != null) {
                System.out.println(coro);
                throw new NotImplementedException();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            assert lockOwner == thread && lockRecursive == 0;
            terminated = true;
            unlock();
        }
    }

    void symmetricYield() {
        if (false && TRACE) {
            System.out.println("locking for symmetric yield...");
        }

        lock();
        Coroutine next = currentCoroutine.next;
        if (next == currentCoroutine) {
            unlock();
            return;
        }

        if (false && TRACE) {
            System.out.println("symmetric yield to " + next);
        }

        final Coroutine current = currentCoroutine;
        currentCoroutine = next;

        unlockLater(next);
        switchTo(current, next);
        beforeResume(current);
    }

    void symmetricYieldTo(Coroutine target) {
        lock();
        if (target.threadSupport != this) {
            unlock();
            return;
        }
        moveCoroutine(currentCoroutine, target);

        final Coroutine current = currentCoroutine;
        currentCoroutine = target;
        unlockLater(target);
        switchTo(current, target);
        beforeResume(current);
    }

    private void moveCoroutine(Coroutine a, Coroutine position) {
        // remove a from the ring
        a.last.next = a.next;
        a.next.last = a.last;

        // ... and insert at the new position
        a.next = position.next;
        a.last = position;
        a.next.last = a;
        position.next = a;
    }

    void symmetricStopCoroutine(Coroutine target) {
        Coroutine current;
        lock();
        try {
            if (target.threadSupport != this) {
                unlock();
                return;
            }
            moveCoroutine(currentCoroutine, target);

            current = currentCoroutine;
            currentCoroutine = target;
        } finally {
            unlock();
        }
        switchToAndExit(current, target);
    }


    /**
     * switch to coroutine and throw Exception in coroutine
     */
    private void symmetricExitInternal(Coroutine coroutine) {
        assert currentCoroutine != coroutine;
        assert coroutine.threadSupport == this;

        // remove the coroutine from the ring
        coroutine.last.next = coroutine.next;
        coroutine.next.last = coroutine.last;

        if (!testDisposableAndTryReleaseStack(coroutine.data)) {
            // and insert it before the current coroutine
            coroutine.last = currentCoroutine.last;
            coroutine.next = currentCoroutine;
            coroutine.last.next = coroutine;
            currentCoroutine.last = coroutine;

            final Coroutine current = currentCoroutine;
            currentCoroutine = coroutine;
            switchToAndExit(current, coroutine);
            beforeResume(current);
        }
    }


    /**
     * terminate current coroutine and yield forward
     */
    void terminateCoroutine() {
        assert currentCoroutine != threadCoroutine : "cannot exit thread coroutine";
        assert currentCoroutine != currentCoroutine.next : "last coroutine shouldn't call coroutineexit";

        lock();
        Coroutine old = currentCoroutine;
        Coroutine forward = old.next;
        currentCoroutine = forward;
        old.last.next = old.next;
        old.next.last = old.last;

        if (false && DEBUG) {
            System.out.println("to be terminated: " + old);
        }
        unlockLater(forward);
        switchToAndTerminate(old, forward);
    }

    /**
     * Steal coroutine from it's carrier thread to current thread.
     *
     * @param failOnContention steal fail if there's too much lock contention
     * @param coroutine        to be stolen
     */
    boolean steal(Coroutine coroutine, boolean failOnContention) {
        assert coroutine.threadSupport.threadCoroutine() != coroutine;
        CoroutineSupport source = this;
        CoroutineSupport target = SharedSecrets.getJavaLangAccess().currentThread0().getCoroutineSupport();

        if (source == target) {
            return true;
        }

        if (source.id < target.id) { // prevent dead lock
            if (!source.lockInternal(failOnContention)) {
                return false;
            }
            target.lock();
        } else {
            target.lock();
            if (!source.lockInternal(failOnContention)) {
                target.unlock();
                return false;
            }
        }

        try {
            try {
                if (source.terminated || coroutine.finished ||
                        coroutine.threadSupport != source || // already been stolen
                        source.currentCoroutine == coroutine || // running
                        !stealCoroutine(coroutine.data)) { // native frame
                    return false;
                }

                coroutine.last.next = coroutine.next;
                coroutine.next.last = coroutine.last;
                coroutine.threadSupport = target;
            } finally {
                source.unlock();
            }
            coroutine.next = target.currentCoroutine.next;
            coroutine.last = target.currentCoroutine;
            target.currentCoroutine.next = coroutine;
            coroutine.next.last = coroutine;
        } finally {
            target.unlock();
        }

        return true;
    }

    /**
     * Can not be stolen while executing this, because lock is held
     */
    void beforeResume(CoroutineBase source) {
        if (source.needsUnlock) {
            source.needsUnlock = false;
            source.threadSupport.unlock();
        }
    }

    private void unlockLater(CoroutineBase next) {
        if (CHECK_LOCK && next.needsUnlock) {
            throw new InternalError("pending unlock");
        }
        next.needsUnlock = true;
    }

    private void lock() {
        boolean success = lockInternal(false);
        assert success;
    }

    private boolean lockInternal(boolean tryingLock) {
        final Thread th = SharedSecrets.getJavaLangAccess().currentThread0();
        if (lockOwner == th) {
            lockRecursive++;
            return true;
        }
        for (int spin = 1; ; ) {
            if (lockOwner == null && LOCK_UPDATER.compareAndSet(this, null, th)) {
                return true;
            }
            for (int i = 0; i < spin; ) {
                i++;
            }
            if (spin == SPIN_BACKOFF_LIMIT) {
                if (tryingLock) {
                    return false;
                }
                SharedSecrets.getJavaLangAccess().yield0(); // yield safepoint
            } else { // back off
                spin *= 2;
            }
        }
    }

    private void unlock() {
        if (CHECK_LOCK && SharedSecrets.getJavaLangAccess().currentThread0() != lockOwner) {
            throw new InternalError("unlock from non-owner thread");
        }
        if (lockRecursive > 0) {
            lockRecursive--;
        } else {
            LOCK_UPDATER.lazySet(this, null);
        }
    }

    private static final AtomicReferenceFieldUpdater<CoroutineSupport, Thread> LOCK_UPDATER;

    static {
        LOCK_UPDATER = AtomicReferenceFieldUpdater.newUpdater(CoroutineSupport.class, Thread.class, "lockOwner");
    }

    /**
     * @return current running coroutine
     */
    public CoroutineBase getCurrent() {
        return currentCoroutine;
    }

    private static native void registerNatives();

    private static native long getThreadCoroutine();

    /**
     * need lock because below methods will operate on thread->coroutine_list()
     */
    private static native long createCoroutine(CoroutineBase coroutine, long stacksize);

    @HotSpotIntrinsicCandidate
    private static native void switchToAndTerminate(CoroutineBase current, CoroutineBase target);

    private static native boolean testDisposableAndTryReleaseStack(long coroutine);

    private static native boolean stealCoroutine(long coroPtr);
    // end of locking

    @HotSpotIntrinsicCandidate
    private static native void switchTo(CoroutineBase current, CoroutineBase target);

    @HotSpotIntrinsicCandidate
    private static native void switchToAndExit(CoroutineBase current, CoroutineBase target);

    private static native CoroutineBase cleanupCoroutine();

    /**
     * Telling jvm that wisp is ready to be used.
     */
    public static native void setWispBooted();
}
