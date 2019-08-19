/*
 * @test
 * @library /lib/testlibrary
 * @summary verification of work stealing really happened
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+UseWisp2 -Dcom.alibaba.wisp.schedule.stealRetry=100 Wisp2WorkStealTest
 */

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertNE;

public class Wisp2WorkStealTest {
    public static void main(String[] args) {
        Object lock = new Object();
        AtomicReference<Thread> t = new AtomicReference<>();
        AtomicReference<Thread> t2 = new AtomicReference<>();
        WispEngine.dispatch(() -> {
            t.set(SharedSecrets.getJavaLangAccess().currentThread0());
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            t2.set(SharedSecrets.getJavaLangAccess().currentThread0());
        });

        // letting a coroutine occupy the carrier
        while (true) {
            AtomicReference<Boolean> found = new AtomicReference<>(null);
            WispEngine.dispatch(() -> {
                if (SharedSecrets.getJavaLangAccess().currentThread0() == t.get()) {
                    found.set(true);
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < 1000) {
                        // occupy the carrier
                    }
                } else {
                    found.set(false);
                }
            });
            while (found.get() == null) {
            }
            if (found.get()) {
                break;
            }
        }

        synchronized (lock) {
            lock.notify();
        }

        while (t2.get() == null) {
        }

        assertNE(t.get(), t2.get());
    }
}
