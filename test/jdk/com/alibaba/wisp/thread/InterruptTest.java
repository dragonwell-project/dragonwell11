/*
 * @test
 * @summary test thread.interrupt() of wispTask
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true InterruptTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 InterruptTest
 */

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CountDownLatch;

public class InterruptTest {
    public static void main(String[] args) throws Exception {
        Lock lock = new ReentrantLock();
        Condition dummy = lock.newCondition();
        Condition finish = lock.newCondition();
        Condition threadSet = lock.newCondition();
        CountDownLatch finishLatch = new CountDownLatch(1);

        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicReference<Thread> thrd = new AtomicReference<>();

        WispEngine.dispatch(() -> {
            thrd.set(Thread.currentThread());
            lock.lock();
            finishLatch.countDown();
            threadSet.signal();
            try {
                try {
                    dummy.await(1, TimeUnit.SECONDS);
                    throw new Error("Exception not happened");
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            } finally {
                finish.signal();
                lock.unlock();
            }
        });

        finishLatch.await();
        lock.lock();
        try {
            if (thrd.get() == null && !threadSet.await(1, TimeUnit.SECONDS))
                throw new Error("wait threadSet");
        } finally {
            lock.unlock();
        }

        if (thrd.get() == null)
            throw new Error("thread not set");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        thrd.get().interrupt();

        lock.lock();
        try {
            if (!finish.await(1, TimeUnit.SECONDS))
                throw new Error("wait finish");
        } finally {
            lock.unlock();
        }

        if (!interrupted.get())
            throw new Error("InterruptedException not happened");

    }
}
