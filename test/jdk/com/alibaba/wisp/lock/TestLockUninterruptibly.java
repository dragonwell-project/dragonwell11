/*
 * @test
 * @library /lib/testlibrary
 * @summary Test to verify we are not spinning when we're trying to acquire monitor with interrupted status
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestLockUninterruptibly
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestLockUninterruptibly
*/

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestLockUninterruptibly {

    public static void main(String[] args) throws Exception {
        Object lock = new Object();

        synchronized (lock) {
            AtomicReference<WispEngine> engine = new AtomicReference<>();

            new Thread(() -> {
                engine.set(WispEngine.current());
                Thread.currentThread().interrupt();
                synchronized (lock) {
                  // now we're consume 100% CPU
                  // because nio can not block the
                  // thread(coroutine) in a interrupted status,
                  // as a result we're spinning in a fetching loop
                }
            }).start();

            while (engine.get() == null) {
            }

            int loops = getEventLoops(engine.get());

            Thread.sleep(100);

            assertTrue(getEventLoops(engine.get()) < loops + 10);
        }
    }


    private static int getEventLoops(WispEngine engine) throws Exception {
        Field f = WispEngine.class.getDeclaredField("statistics");
        f.setAccessible(true);
        Object statistics = f.get(engine);
        f = Class.forName(WispEngine.class.getName() + "$Statistics")
                .getDeclaredField("eventLoops");
        f.setAccessible(true);
        return f.getInt(statistics);
    }
}
