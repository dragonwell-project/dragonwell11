/*
 * @test
 * @library /lib/testlibrary
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @summary verify canceled timers are removed ASAP
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 TestWisp2TimerRemove
 */

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;
import java.lang.reflect.Field;
import java.util.concurrent.*;

import static jdk.testlibrary.Asserts.*;

public class TestWisp2TimerRemove {

    public static void main(String[] args) throws Exception {
        BlockingQueue<Integer> q1 = new ArrayBlockingQueue<>(1);
        BlockingQueue<Integer> q2 = new ArrayBlockingQueue<>(1);
        ExecutorService es = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(2);

        final int WORKERS = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < WORKERS; i++) {
            es.submit(() -> {
                TimeUnit.MINUTES.sleep(10);
                return null;
            });
        }

        es.submit(() -> pingpong(q1, q2, latch));
        es.submit(() -> pingpong(q2, q1, latch));
        q1.offer(1);
        latch.await();

        for (int i = 0; i < WORKERS * 10; i++) { // iterate all carriers
            Integer ql = es.submit(() -> {
                WispEngine engine = WispEngine.current();
                TimeUnit.MILLISECONDS.sleep(1);
                assertEQ(engine, WispEngine.current());
                return new FieldAccessor(SharedSecrets.getJavaLangAccess().getWispTask(Thread.currentThread()))
                        .access("carrier")
                        .access("worker")
                        .access("timerManager")
                        .access("queue")
                        .getInt("size");
            }).get();
            // ThreadPoolExecutor itself could be waiting on timer
            assertLessThanOrEqual(ql, WORKERS * 2 - 1);
        }
    }

    private static Void pingpong(BlockingQueue<Integer> pingQ,
                                 BlockingQueue<Integer> pongQ,
                                 CountDownLatch latch) throws Exception {
        for (int i = 0; i < 100000; i++) {
            pingQ.poll(1, TimeUnit.HOURS);
            pongQ.offer(1);
        }
        stealInfo();
        latch.countDown();
        return null;
    }

    private static void stealInfo() throws ReflectiveOperationException {
        int stealCount = new FieldAccessor(SharedSecrets.getWispEngineAccess().getCurrentTask())
                .getInt("stealCount");
        System.out.println("stealCount = " + stealCount);
    }

    static class FieldAccessor {
        final Object obj;

        FieldAccessor(Object o) {
            this.obj = o;
        }

        FieldAccessor access(String fieldName) throws ReflectiveOperationException {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return new FieldAccessor(field.get(obj));
        }

        int getInt(String fieldName) throws ReflectiveOperationException {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(obj);
        }
    }
}
