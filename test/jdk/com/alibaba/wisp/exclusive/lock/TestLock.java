/*
 * @test
 * @summary Test ReentrantLock in coroutine environment
 * @modules java.base/jdk.internal.misc
 * @library /lib/testlibrary
 * @requires os.family == "linux"
 * @run main/othervm  -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true TestLock
*/


import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestLock {
    static Lock lock = new ReentrantLock();
    static Condition cond = lock.newCondition();
    static CountDownLatch latch = new CountDownLatch(2);
    static CountDownLatch p1Locked = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        WispEngine.dispatch(TestLock::p1);
        WispEngine.dispatch(TestLock::p2);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    private static void assertInterval(long start, int diff, int bias) {
        long cur = System.currentTimeMillis();
        long v = Math.abs(cur - start - diff);
        if (v > bias) {
            throw new Error("not wakeup expected");
        }
    }

    private static void p1() {
        lock.lock();
        p1Locked.countDown();
        SharedSecrets.getWispEngineAccess().sleep(100);
        try {
            long start = System.currentTimeMillis();
            cond.await();
            assertInterval(start, 100, 5);

        } catch (InterruptedException e) {
           throw new Error();
        } finally {
            lock.unlock();
        }

        latch.countDown();
    }

    private static void p2() {
        try {
            p1Locked.await();
            long start = System.currentTimeMillis();
            lock.lock();

            assertInterval(start, 100, 5);
            SharedSecrets.getWispEngineAccess().sleep(100);
            cond.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        latch.countDown();
    }


}
