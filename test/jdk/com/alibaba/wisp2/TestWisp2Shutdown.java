/*
 * @test
 * @summary TestWisp2Shutdown
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestWisp2Shutdown
 */

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static jdk.testlibrary.Asserts.assertEQ;

public class TestWisp2Shutdown {
    public static void main(String[] args) throws Exception {
        AtomicInteger n = new AtomicInteger();

        Constructor<?> c = Class.forName("com.alibaba.wisp.engine.Wisp2Group")
                .getDeclaredConstructor(int.class, ThreadFactory.class);
        c.setAccessible(true);
        ExecutorService g = (ExecutorService) c.newInstance(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r);
            }
        });

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

        g.awaitTermination(1, TimeUnit.SECONDS);

        System.out.println(System.currentTimeMillis() - start + "ms");

        assertEQ(n.get(), 0);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
