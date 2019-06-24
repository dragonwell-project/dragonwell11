/*
 * @test
 * @summary Test ReentrantLock in coroutine environment
 * @modules java.base/jdk.internal.misc
 * @run main/othervm  -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 LockTest
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockTest {
    static Lock lock = new ReentrantLock();
    static Condition cond = lock.newCondition();

    public static void main(String[] args) {
        WispEngine.dispatch(LockTest::p1);
        WispEngine.dispatch(LockTest::p2);

        SharedSecrets.getWispEngineAccess().eventLoop();

        System.out.println(lock);

        new Thread(LockTest::p1).start();
        SharedSecrets.getWispEngineAccess().sleep(1);
        new Thread(LockTest::p2).start();
    }

    public static void assertInterval(long start, int diff, int bias) {
        if (Math.abs(System.currentTimeMillis() - start - diff) > bias)
            throw new Error("not wakeup expected");
    }

    private static void p1() {

        lock.lock();
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


    }

    private static void p2() {

        long start = System.currentTimeMillis();
        lock.lock();
        try {
            assertInterval(start, 100, 5);
            SharedSecrets.getWispEngineAccess().sleep(100);
            cond.signal();
        } finally {
            lock.unlock();
        }
    }


}
