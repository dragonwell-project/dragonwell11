/*
 * @test
 * @summary Test WispEngine's destroy
 * @modules java.base/jdk.internal.misc java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestWispDestroy
*/

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import jdk.internal.misc.SharedSecrets;


public class TestWispDestroy {

    static Object e;

    public static void main(String[] args) throws Exception {
        CountDownLatch l = new CountDownLatch(1);

        new Thread(() -> {
            try {
                Field f = WispTask.class.getDeclaredField("carrier");
                f.setAccessible(true);
                e = f.get(SharedSecrets.getJavaLangAccess().getWispTask(Thread.currentThread()));
                l.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        l.await();

        Thread.sleep(1000); // ensure Thread.exit() executed


        Field terminated = e.getClass().getDeclaredField("terminated");
        terminated.setAccessible(true);
        if (!terminated.getBoolean(e)) throw new Error("resource leak!");
    }
}
