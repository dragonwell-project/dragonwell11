/*
 * @test
 * @library /lib/testlibrary
 * @build TestKillThreads RcmUtils
 * @summary test RCM TestKillThreads
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -XX:+Wisp2ThreadStop -XX:ActiveProcessorCount=4 TestKillThreads
 */

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceType;
import com.alibaba.rcm.internal.RCMUnsafe;
import com.alibaba.wisp.engine.WispResourceContainerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jdk.testlibrary.Asserts.assertTrue;
import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.fail;

public class TestStressedKillThreads {

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

    private static Runnable TIMER = () -> {
        try {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                }
            }, 1, 1);
            started = 1;
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            fail();
        } finally {
            flag.set(true);
        }
    };

    private static Runnable UPDATER = () -> {
        boolean running = true;
        started = 1;
        int preTimes = -1;
        int version = 0;
        AtomicInteger checkTimes = new AtomicInteger(0);
        while (running) {
            try {
                version = 0;
                AtomicInteger atomicInteger = checkTimes;
                synchronized (atomicInteger) {
                    while (preTimes == checkTimes.get() && running) {
                        checkTimes.wait();
                    }
                    preTimes = checkTimes.get();
                }
                if (!running) continue;
            } catch (Exception e) {
                fail();
            }
        }
    };

    private static Runnable POLL = () -> {
        DelayQueue<Delayed> taksQueue = new DelayQueue();
        while (true) {
            started = 1;
            try {
                while (true) {
                    Delayed task =taksQueue.take();
                    System.out.println(taksQueue);
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
                continue;
            }
        }
    };

    private static Runnable OVERRIDE = () -> {
        while (true) {
            started = 1;
            try {
                try {
                    Thread.sleep(200);
                } finally {
                    throw new Exception("123");
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }

        }
    };

    private static final Object lock = new Object();

    public static Runnable OBJECTMONITOR = () -> {
        started = 1;
        while (true) {
            synchronized (lock) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                }
            }
        }
    };

    public static Runnable INLINE_C2 = () -> {
        started =1;
        while (true) {
            try {
                f();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    };

    private static void f() {
    }

    private static final List<Runnable> workload = new ArrayList<Runnable>() {{
        add(INLINE_C2);
    }};

    private static final long sl = TimeUnit.SECONDS.toMillis(Integer.parseInt(System.getProperty("sl", "5")));

    public static void main(String[] args) throws Exception {
        for (Runnable runnable : workload) {
            ResourceContainer container = RcmUtils.createContainer(Collections.singletonList(
                    ResourceType.CPU_PERCENT.newConstraint(40)));
            started = 0;
            container.run(() -> {
                Thread t = new Thread(runnable);
                t.start();
            });
            while (0 == started) {
            }
            Thread.sleep(sl);
            RCMUnsafe.killThreads(container);
            container.destroy();
            System.out.println("pass");
        }
}
