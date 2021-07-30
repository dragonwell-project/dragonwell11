/*
 * @test
 * @summary Test case for fix a deadlock caused by critical section of WispEngine
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm   -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.globalPoller=false WispEngineCriticalSectionTest
 */


import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.channels.spi.AbstractSelector;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class WispEngineCriticalSectionTest {
    public static void main(String[] args) throws Exception {
        // do blocking operation and register to engine
        Socket so = new Socket("www.example.com", 80);

        so.close(); // put into engine's cancel queue

        if (!Class.forName("com.alibaba.wisp.engine.ScheduledWispEngine").isInstance(WispEngine.current())) {
            return;
        }
        Field f = Class.forName("com.alibaba.wisp.engine.ScheduledWispEngine").getDeclaredField("selector");
        f.setAccessible(true);
        AbstractSelector selector = (AbstractSelector) f.get(WispEngine.current());
        f = AbstractSelector.class.getDeclaredField("cancelledKeys");
        f.setAccessible(true);
        final HashSet cancelSet = (HashSet) f.get(selector);


        // made an runnable wisp
        CountDownLatch cd = new CountDownLatch(1);
        WispEngine.dispatch(() -> { // coroutine A
            try {
                cd.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // finally let the engine select...
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //
        });
        cd.countDown();
        // coroutine A is runnable


        CountDownLatch lock = new CountDownLatch(1);
        new Thread(() -> {
            synchronized (lock) {
                lock.countDown();
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        lock.await();
        // now above thread holds lock(CountDownLatch).

        SharedSecrets.getWispEngineAccess().runInCritical(() -> {
            //deadlock if we don't put the below logic inside critical section of WispEngine
            synchronized (cancelSet) {
                synchronized (lock) {
            /*
                If without protection of critical section, attempting to acquire lock will yield to coroutine A,
                which continues after "cd.await();", then deadlocks when the processDeregisterQueue
                (trigger by Thread.sleep) is trying to acquire cancelSet lock
            */

                }
            }
            return null;
        });
    }
}
