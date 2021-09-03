/*
 * @test
 * @summary test submit task to engine.
 * @library /lib/testlibrary
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestEngineExecutor
*/

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestEngineExecutor {
    public static void main(String[] args) throws Exception {
        testExecutor();
    }

    static AtomicReference<WispEngine> engine = new AtomicReference<>();
    private static void testExecutor() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            engine.set(WispEngine.current());
            latch.countDown();
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
        latch.await();
        WispEngine e = engine.get();
        CountDownLatch latch1 = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            e.execute(latch1::countDown);
        }
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
    }
}
