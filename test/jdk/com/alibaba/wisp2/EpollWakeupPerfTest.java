/*
 * @test
 * @summary test selector.wakeup() performance
 * @run main/othervm -XX:+UseWisp2  EpollWakeupPerfTest
 */

import java.nio.channels.Selector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EpollWakeupPerfTest {
    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();
        AtomicLong wakenedTs = new AtomicLong();
        CyclicBarrier barrier = new CyclicBarrier(2);

        Thread io = new Thread(() -> {
            try {
                while (true) {
                    barrier.await();
                    selector.select();
                    wakenedTs.lazySet(System.nanoTime());
                    synchronized (lock) {
                        lock.notify();
                    }
                }

            } catch (Throwable t) {
            }
        }, "IO thread");

        io.start();

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            System.out.println("tested " + k + " times, " +
                    "average latency=" + m + "ns");
            synchronized (lock) {
                m = k = 0;
            }
        }, 1, 1, TimeUnit.SECONDS);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3_000) {
            wakenedTs.set(0);
            selector.selectNow(); // reset
            barrier.await(); // start...
            new CountDownLatch(1).await(100, TimeUnit.MICROSECONDS); // ensure select()
            long wakeupTs = System.nanoTime();
            selector.wakeup();

            synchronized (lock) {
                while (wakenedTs.get() == 0) {
                    lock.wait();
                }
            }
            long latency = wakenedTs.get() - wakeupTs;
            if (latency > 0) {
                synchronized (lock) {
                    m += k++ == 0 ? latency : (latency - m) / k;
                }
            }
        }
    }

    private static long m, k;
    private final static Object lock = new Object();
}
