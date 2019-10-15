/*
 * @test
 * @summary test dispatching thread into our managed workers
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.threadPoolLimit=true -Dcom.alibaba.wisp.enableThreadAsWisp=true TestThreadAsWisp
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.threadPoolLimit=true -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.version=2 -XX:ActiveProcessorCount=4 TestThreadAsWisp
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispWorkerContainer;
import jdk.internal.misc.SharedSecrets;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import static jdk.testlibrary.Asserts.assertEQ;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestThreadAsWisp {
    static Thread mainThread;

    private static Lock lock = new ReentrantLock();
    private static Condition cond = lock.newCondition();
    private static BiConsumer<Executor, CountDownLatch> consumer = (es, counter) -> {
        es.execute(() -> {
            try {
                Thread.sleep(1_000L);
                counter.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    };
    private static Executor executor;
    private static CountDownLatch end;
    private static int wisp_version;

    public static void main(String[] args) throws Exception {
        // create real java threads at test beginning
        final CountDownLatch begin = new CountDownLatch(5);
        IntStream.range(0, 5).forEach(i -> new Thread(() -> {
            try {
                lock.lock();
                begin.countDown();
                cond.await();
                consumer.accept(executor, end);
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "Thread ~ " + i).start());

        begin.await();

        File f = File.createTempFile("wisp-", ".config");
        System.setProperty("com.alibaba.wisp.config", f.getAbsolutePath());
        f.deleteOnExit();
        FileWriter writer = new FileWriter(f);
        writer.write("com.alibaba.wisp.biz.manage=TestThreadAsWisp::main\n");
        writer.write("com.alibaba.wisp.biz.current=TestThreadAsWisp::main\n");
        writer.write("com.alibaba.wisp.biz.black=TestThreadAsWisp::shouldNotShift\n");

        writer.close();

        // WispBizSniffer has already been loaded;
        // reload WispBizSniffer's config from file.
        Method m = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredMethod("loadBizConfig");
        m.setAccessible(true);
        m.invoke(null);

        Class<?> wispClazz = Class.forName("com.alibaba.wisp.engine.WispConfiguration");
        Field field = wispClazz.getDeclaredField("WISP_VERSION");
        field.setAccessible(true);
        wisp_version = field.getInt(null);

        AtomicBoolean success = new AtomicBoolean(true);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Thread> t = new AtomicReference<>();

        // test Thread.start()
        t.set(new Thread(() -> {
            Thread realThread = SharedSecrets.getJavaLangAccess().currentThread0();
            boolean value = t.get() == Thread.currentThread()
                    && !SharedSecrets.getWispEngineAccess().isThreadTask(
                    SharedSecrets.getWispEngineAccess().getCurrentTask())
                    && Thread.currentThread().getName().startsWith("new_thread");
            success.set(value);
            done.countDown();

        }, "new_thread"));

        t.get().start();
        done.await();

        assertTrue(success.get());

        // test tread pool
        CountDownLatch done1 = new CountDownLatch(1);
        final String NAME = "ShiftThreadModelTest-pool";
        ExecutorService es = Executors.newCachedThreadPool(r -> {
            Thread t1 = new Thread(r);
            t1.setName(NAME);
            return t1;
        });
        mainThread = SharedSecrets.getJavaLangAccess().currentThread0();
        es.execute(() -> {
            Thread realThread = SharedSecrets.getJavaLangAccess().currentThread0();
            if (wisp_version == 2) {
                success.set(realThread != mainThread
                    && Thread.currentThread() != realThread
                    && Thread.currentThread().getClass().getSimpleName().contains("WispThreadWrapper")
                    && Thread.currentThread().getName().startsWith(NAME)
                    && !SharedSecrets.getWispEngineAccess().isThreadTask(
                        SharedSecrets.getWispEngineAccess().getCurrentTask())
                );
            } else {
                success.set(realThread == mainThread
                    && Thread.currentThread() != realThread
                    && Thread.currentThread().getClass().getSimpleName().contains("WispThreadWrapper")
                    && Thread.currentThread().getName().startsWith(NAME)
                    && !SharedSecrets.getWispEngineAccess().isThreadTask(
                        SharedSecrets.getWispEngineAccess().getCurrentTask())
                );
            }
            done1.countDown();
        });
        done1.await();
        assertTrue(success.get());

        shouldNotShift();

        // test thread pool wisp limit
        testThreadPoolLimit(new LinkedBlockingQueue(), 3, 5, false);
        testThreadPoolLimit(new LinkedBlockingQueue(3), 3, 10, true);
        testThreadPoolLimit(new LinkedBlockingQueue(3), 3, 6, false);
        System.out.println("LinkedBlockingQueue ok!");
        testThreadPoolLimit(new LinkedBlockingDeque(), 3, 1, false);
        testThreadPoolLimit(new LinkedBlockingDeque(3), 3, 10, true);
        testThreadPoolLimit(new LinkedBlockingDeque(3), 3, 6, false);
        System.out.println("LinkedBlockingDeque ok!");
        testThreadPoolLimit(new ArrayBlockingQueue(3), 3, 10, true);
        testThreadPoolLimit(new ArrayBlockingQueue(3), 3, 6, false);
        System.out.println("ArrayBlockingQueue ok!");
        testThreadPoolLimit(new SynchronousQueue(), 3, 6, true);
        testThreadPoolLimit(new SynchronousQueue(), 3, 3, false);
        System.out.println("SynchronousQueue ok!");
        testThreadPoolLimit(new DelayQueue(), 1, 3, false);
        System.out.println("DelayQueue ok!");
        testThreadPoolLimit(new LinkedTransferQueue(), 1, 3, false);
        testThreadPoolLimit(new LinkedTransferQueue(), 3, 3, false);
        System.out.println("LinkedTransferQueue ok!");
        testThreadPoolLimit(new PriorityBlockingQueue(), 1, 3, false);
        System.out.println("PriorityBlockingQueue ok!");

        // multi-thread test
        RejectedExecutionHandler rejectHandler = (r, executor) -> {
            success.set(true);
        };
        executor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(1), rejectHandler);
        success.set(false);
        end = new CountDownLatch(1);
        lock.lock();
        cond.signalAll();
        lock.unlock();
        end.await();
        assertTrue(success.get());
    }

    private static void testThreadPoolLimit(BlockingQueue queue, int maxPoolSize, int submitCount, boolean expect) throws InterruptedException {
        AtomicBoolean success = new AtomicBoolean(false);
        RejectedExecutionHandler rejectHandler = (r, executor) -> {
            success.set(true);
        };
        executor = new ThreadPoolExecutor(1, maxPoolSize, Long.MAX_VALUE, TimeUnit.SECONDS,
                queue, rejectHandler);
        end = new CountDownLatch(1);
        IntStream.range(0, submitCount).forEach(i -> {
            consumer.accept(executor, end);
        });
        end.await();
        assertEQ(success.get(), expect);
    }

    // test black list
    static private void shouldNotShift() throws Exception {
        // even we're in the main() method,  the thread model should not be changed,
        // because we're in the black list
        ExecutorService es = Executors.newCachedThreadPool();
        AtomicBoolean success = new AtomicBoolean(true);
        CountDownLatch done = new CountDownLatch(1);
        es.execute(() -> {
            Thread realThread = SharedSecrets.getJavaLangAccess().currentThread0();
            success.set(realThread != mainThread &&
                    !WispWorkerContainer.INSTANCE.getWorkers().stream().map(e -> {
                        try {
                            Field f = WispEngine.class.getDeclaredField("thread");
                            f.setAccessible(true);
                            return f.get(e);
                        } catch (Exception e1) {
                            throw new Error();
                        }
                    }).collect(Collectors.toList()).contains(realThread));
            done.countDown();
        });
        done.await();
        assertTrue(success.get());
    }
}
