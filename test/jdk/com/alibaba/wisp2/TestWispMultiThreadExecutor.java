/*
 * @test
 * @summary Test WispCounter using by WispMultiThreadExecutor
 * @library /lib/testlibrary
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine  -Dcom.alibaba.wisp.profile=true -Dcom.alibaba.wisp.transparentWispSwitch=true -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableHandOff=false  TestWispMultiThreadExecutor
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableHandOff=false  TestWispMultiThreadExecutor
*/

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.management.WispCounterData;
import com.alibaba.management.WispCounterMXBean;

import javax.management.MBeanServer;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestWispMultiThreadExecutor {
    static WispMultiThreadExecutor executor;
    static WispCounterMXBean mbean;
    static Method counterMethod;
    static boolean isNewJdk;
    public static void main(String[] args) throws Exception {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbean = ManagementFactory.newPlatformMXBeanProxy(mbs,
                    "com.alibaba.management:type=WispCounter", WispCounterMXBean.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Method getCounter = WispCounterMXBean.class.getDeclaredMethod("getWispCounter", long.class);

        if (getCounter.getReturnType() == WispCounterData.class) {
            isNewJdk = true;
        }
        testWisp2Group();
    }

    private static void testWisp2Group() throws Exception {
        executor = new WispMultiThreadExecutor(4, new ThreadFactory() {
            AtomicInteger seq = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Wisp2-Group-Test-Carrier-" + seq.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
        Thread.sleep(100);
        for (int j = 0; j < 10; ++j) {
            executor.execute(()-> {});
        }
        List<Boolean> list = mbean.getRunningStates();
        System.out.println(list);
        int size1 = list.size();
        List<Long> list2 = mbean.getCreateTaskCount();
        System.out.println(list2);
        list2 = mbean.getExecutionCount();
        System.out.println(list2);
        List<Long> engines = executor.getWispEngines();
        System.out.println(engines);
        int count = 0;
        for (Long id: engines) {
            if (isNewJdk) {
                WispCounterData data = mbean.getWispCounter(id);
                if (data != null) {
                    System.out.println(data.getExecutionCount());
                }
                count++;
            }
        }
        assertTrue(count == 4);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        list = mbean.getRunningStates();
        System.out.println(list);
        int size2 = list.size();
        assertTrue((size1 - size2) == 4);
    }

    static class WispMultiThreadExecutor extends AbstractExecutorService {
        private final WispEngine delegated;
        public WispMultiThreadExecutor(int threadCount, ThreadFactory threadFactory) {
            delegated = WispEngine.createEngine(threadCount, threadFactory);
        }

        @Override
        public void execute(Runnable command) {
            delegated.execute(command);
        }

        @Override
        public void shutdown() {
            delegated.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegated.awaitTermination(timeout, unit);
        }

        public List<Long> getWispEngines() {
            return delegated.getWispCarrierIds();
        }
    }
}
