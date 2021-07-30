/*
 * @test
 * @summary test thread.isAlive() of wispTask
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.enableThreadAsWisp=true IsAliveTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.version=2 IsAliveTest
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


public class IsAliveTest {
    public static void main(String[] args) throws Exception {
        testCurrentDispatch();
        testContainerDispatch();
    }

    private static void testCurrentDispatch() {
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

    private static void testContainerDispatch() throws Exception {
        Field f = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredField("PUT_TO_MANAGED_THREAD_STACK_LIST");
        f.setAccessible(true);
        f.set(null, Collections.singletonList("IsAliveTest::testContainerDispatch"));
        AtomicBoolean finish = new AtomicBoolean();
        Thread t = new Thread(() -> finish.set(true));
        t.start();

        assertTrue(t.isAlive() || finish.get());
    }
}
