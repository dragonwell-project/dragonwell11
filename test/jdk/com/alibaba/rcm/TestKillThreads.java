/*
 * @test
 * @library /lib/testlibrary
 * @build TestRcmCpu RcmUtils
 * @summary test RCM TestKillThreads
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main/othervm -XX:+UseWisp2 -XX:ActiveProcessorCount=4 TestKillThreads
 */
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceType;
import com.alibaba.rcm.internal.RCMUnsafe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static jdk.testlibrary.Asserts.assertTrue;
import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.fail;

public class TestKillThreads {

    private static volatile int started = 0;
    private static AtomicBoolean flag = new AtomicBoolean(false);

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

    private static Runnable PARK = () -> {
        try {
            started = 1;
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            fail();
        }
    };

    private static final List<Runnable> workload = new ArrayList<Runnable>() {{
//        add(VOID_LOOP); TODO:// support  loop
//        add(BUSY_LOOP);
        add(PARK);
        add(FINALLY);
    }};

    public static void main(String[] args) {
        for (Runnable runnable : workload) {
            ResourceContainer container = RcmUtils.createContainer(
                    ResourceType.CPU_PERCENT.newConstraint(40));
            started = 0;
            container.run(() -> {
                Thread t =  new Thread(runnable);
                t.start();
            });
            while(0 == started) {}
            RCMUnsafe.killThreads(container);
            container.destroy();
        }
        assertTrue(flag.get());
    }
}
