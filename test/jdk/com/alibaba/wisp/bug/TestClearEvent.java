/*
 * @test
 * @summary Explain the fix of T11748781
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestClearEvent
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestClearEvent
*/

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

public class TestClearEvent {
    private static WispEngineAccess access = SharedSecrets.getWispEngineAccess();

    private static CountDownLatch threadRun = new CountDownLatch(1);
    private static CountDownLatch closed = new CountDownLatch(1);


    public static void main(String[] args) throws Exception {
        Socket so = new Socket("www.example.com", 80);
        so.getOutputStream().write("NOP\n\r\n\r".getBytes());
        // now server returns the data..
        // so is readable
        // current task is interested in read event.
        SocketChannel ch = so.getChannel();
        access.registerEvent(ch, SelectionKey.OP_READ);
        access.park(-1);

        if (!Class.forName("com.alibaba.wisp.engine.ScheduledWispEngine").isInstance(WispEngine.current())) {
            return;
        }
        Field f = Class.forName("com.alibaba.wisp.engine.ScheduledWispEngine").getDeclaredField("selector");
        f.setAccessible(true);
        Selector sel = (Selector) f.get(WispEngine.current());

        new Thread(() -> {
            try {
                threadRun.await();
                so.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            closed.countDown();
        }).start();

        while (true) {
            try {
                cleanEvent(sel, ch);
                break;
            } catch (CancelledKeyException e) {
                continue;
            }
        }
    }

    private static void cleanEvent(Selector sel, SocketChannel ch) throws InterruptedException {
        SelectionKey k = ch.keyFor(sel);
        if (k != null && k.isValid()) {
            threadRun.countDown();
            closed.await();

            // even we checked k.isValid()
            // but if another thread  closed channel here,
            // the next line will produce CancelledKeyException

            try {
                k.interestOps(0);
            } catch (CancelledKeyException e) {
                // channel already closed, do nothing
            }
        }
    }
}
