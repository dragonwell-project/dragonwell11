/*
 * @test
 * @summary Verify wisp internal logic in a WispControlGroup container can be preempted
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+UseWisp2 -Dcom.alibaba.wisp.carrierEngines=4 TestWispControlGroupShutdown
 */


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static jdk.testlibrary.Asserts.assertTrue;
import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.fail;

public class TestWispControlGroupShutdown {
    private static Runnable VOID_LOOP = () -> {
        started = 1;
        while (true) {
        }
    };

    private static Runnable BUSY_LOOP = () -> {
        started = 1;
        while (true) {
            int x;
            do {
                x = 1;
            } while (x == 1);
        }
    };

    private static Runnable PARK = () -> {
        try {
            started = 1;
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            fail();
        }
    };

    private static volatile int started = 0;
    private static AtomicBoolean flag = new AtomicBoolean(false);

    private static Runnable FINALLY = () -> {
        try {
            started = 1;
            new CountDownLatch(1).await();
        } catch (Throwable e) {
            assertTrue(e instanceof ThreadDeath);
        } finally {
            flag.set(true);
        }
    };

    private static final List<Runnable> workload = new ArrayList<Runnable>() {{
//        add(VOID_LOOP); TODO:// support  loop
//        add(BUSY_LOOP);
        add(PARK);
        add(FINALLY);
    }};

    public static void main(String[] args) throws Exception {
        for (Runnable runnable : workload) {
            ExecutorService g = getControlGroup();
            g.execute(runnable);
            started = 0;
            while (0 == started) {}
            g.shutdown();
            try {
                assertTrue(g.awaitTermination(3, TimeUnit.SECONDS));
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
            System.out.println("PASS");
        }
        Thread.sleep(1000);
        assertTrue(flag.get());
    }

    private static ExecutorService getControlGroup() {
        try {
            Class<?> WispControlGroupClazz = Class.forName("com.alibaba.wisp.engine.WispControlGroup");
            Method createMethod = WispControlGroupClazz.getDeclaredMethod("newInstance", int.class);
            createMethod.setAccessible(true);
            return (ExecutorService) createMethod.invoke(null, 50);
        } catch (Exception e) {
           fail();
        }
        return  null;
    }
}
