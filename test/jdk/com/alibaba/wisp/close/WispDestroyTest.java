/*
 * @test
 * @summary Test WispEngine's destroy
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true WispDestroyTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 WispDestroyTest
*/

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Field;
import java.nio.channels.Selector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.nio.channels.ClosedSelectorException;

public class WispDestroyTest {

    static WispEngine e;

    public static void main(String[] args) throws Exception {
        CountDownLatch l = new CountDownLatch(1);

        new Thread(() -> {
            e = WispEngine.current();
            l.countDown();
        }).start();

        l.await();

        Thread.sleep(1000); // ensure Thread.exit() executed

        Field f = WispEngine.class.getDeclaredField("terminated");
        f.setAccessible(true);

        if (!f.getBoolean(e)) throw new Error("resource leak!");
    }
}
