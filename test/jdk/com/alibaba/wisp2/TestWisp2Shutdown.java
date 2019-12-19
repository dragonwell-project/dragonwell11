/*
 * @test
 * @summary TestWisp2Shutdown
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestWisp2Shutdown
 */

import java.lang.reflect.Constructor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispGroup;

import static jdk.testlibrary.Asserts.assertEQ;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestWisp2Shutdown {
    public static void main(String[] args) throws Exception {}

    private static void basicShutDownTest() throws Exception {
        AtomicInteger n = new AtomicInteger();

        WispGroup g = WispGroup.createGroup(4, Thread::new);

        for (int i = 0; i < 888; i++) {
            g.execute(() -> {
                n.incrementAndGet();
                try {
                    while (true) {
                        sleep(1000000);
                    }
                } finally {
                    n.decrementAndGet();
                }
            });
        }

        while (n.get() != 888) {
            n.get();
        }

        long start = System.currentTimeMillis();

        g.shutdown();

        assertTrue(g.awaitTermination(1, TimeUnit.SECONDS));

        System.out.println(System.currentTimeMillis() - start + "ms");

        assertEQ(n.get(), 0);
    }

    private static void multiGroupsShutDownTest() throws Exception {
        CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            WispEngine.dispatch(() -> {
                try {
                    basicShutDownTest();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        }
        assertTrue(latch.await(1, TimeUnit.SECONDS));

    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
