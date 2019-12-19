/*
 * @test
 * @summary test thread.isAlive() of wispTask
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+UseWisp2 TestIsAlive
 *
 */

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.assertTrue;


public class TestIsAlive {
    public static void main(String[] args) throws Exception {
        AtomicBoolean isAlive = new AtomicBoolean();
        AtomicReference<Thread> t = new AtomicReference<>();
        final CountDownLatch cond = new CountDownLatch(1);

        WispEngine.dispatch(() -> {
            t.set(Thread.currentThread());
            isAlive.set(t.get().isAlive());
            cond.countDown();
        });
        try { 
            cond.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(isAlive.get());
        assertFalse(t.get().isAlive());
    }
}
