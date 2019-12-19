/*
 * @test
 * @summary Test elision spin
 * @modules java.base/jdk.internal.misc
 * @library /lib/testlibrary
 * @run main/othervm  -XX:+UseWisp2 -Dcom.alibaba.wisp.carrierEngines=1 TestElisionSpin
 */

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestElisionSpin {
    public static void main(String[] args) {
        assertFalse(SharedSecrets.getWispEngineAccess().hasMoreTasks());

        ReentrantLock lock = new ReentrantLock();
        Condition cond = lock.newCondition();

        WispEngine.dispatch(() -> {
            lock.lock();
            try {
                cond.awaitUninterruptibly();
            } finally {
                lock.unlock();
            }
        });

        lock.lock();
        try {
            cond.signal();
        } finally {
            lock.unlock();
        }
        assertTrue(SharedSecrets.getWispEngineAccess().hasMoreTasks());
    }
}
