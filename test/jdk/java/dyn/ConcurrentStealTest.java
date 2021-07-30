/* @test
 * @summary unit tests for steal in concurrent situation
 * @run junit/othervm/timeout=300 -XX:+EnableCoroutine ConcurrentStealTest
 */

import org.junit.Test;

import java.dyn.Coroutine;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConcurrentStealTest {

    @Test
    public void stealRunning() throws Exception {
        Coroutine[] coro = new Coroutine[1];

        CountDownLatch cdl = new CountDownLatch(1);

        Thread t = new Thread(() -> {
            coro[0] = new Coroutine(() -> {
                try {
                    cdl.countDown();
                    Thread.sleep(10000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            Coroutine.yieldTo(coro[0]);
        }, "stealRunning");
        t.setDaemon(true);
        t.start();

        cdl.await();

        Thread.sleep(500);

        assertFalse(coro[0].steal(true));
    }

    final static int THREADS = Math.min(Runtime.getRuntime().availableProcessors(), 8);
    final static int CORO_PER_TH = 20;
    final static int TIMEOUT = 10;

    @Test
    public void randomSteal() throws Exception {
        Coroutine[] coro = new Coroutine[THREADS * CORO_PER_TH];
        int[] cnt = new int[THREADS];

        AtomicInteger sync = new AtomicInteger();

        for (int th = 1; th < THREADS; th++) {
            int cth = th;
            Thread t = new Thread(() -> {
                for (int i = 0; i < CORO_PER_TH; i++) {
                    coro[CORO_PER_TH * cth + i] = new Coroutine(() -> {
                        while (true) {
                            cnt[cth]++;
                            Coroutine.yield();
                        }
                    });
                }
                sync.incrementAndGet();
                while (sync.get() != THREADS) {
                }

                runRandomCoroutines(System.nanoTime(), coro);
            }, "randomSteal-" + th);
            t.setDaemon(true);
            t.start();
        }

        for (int i = 0; i < CORO_PER_TH; i++) {
            coro[i] = new Coroutine(() -> {
                while (true) {
                    cnt[0]++;
                    Coroutine.yield();
                }
            });
        }

        sync.incrementAndGet();
        while (sync.get() != THREADS) {
        }
        runRandomCoroutines(System.nanoTime(), coro);
        for (int i = 0; i < cnt.length; i++) {
            cnt[i] /= TIMEOUT;
        }
        System.out.println(Arrays.toString(cnt));
    }

    private static void runRandomCoroutines(long start, Coroutine[] coro) {
        while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(TIMEOUT)) {
            Coroutine co = coro[ThreadLocalRandom.current().nextInt(coro.length)];
            if (co.getThread() == Thread.currentThread() || co.steal(true)) {
                Coroutine.yieldTo(co);
            }
        }
    }
}
