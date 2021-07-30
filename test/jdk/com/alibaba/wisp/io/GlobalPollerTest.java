/*
 * @test
 * @library /lib/testlibrary
 * @summary Test for Global Poller
 * @modules java.base/jdk.internal.misc
 * @modules java.base/sun.nio.ch
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.transparentAsync=true GlobalPollerTest
*/

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispPoller;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;
import sun.nio.ch.SelChImpl;

import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static jdk.testlibrary.Asserts.assertTrue;

public class GlobalPollerTest {
    private static WispEngineAccess access = SharedSecrets.getWispEngineAccess();

    public static void main(String[] args) throws Exception {


        Socket so = new Socket("www.example.com", 80);
        so.getOutputStream().write("NOP\n\r\n\r".getBytes());
        // now server returns the data..
        // so is readable
        // current task is interested in read event.
        SocketChannel ch = so.getChannel();
        access.registerEvent(ch, SelectionKey.OP_READ);

        Field f = Class.forName("com.alibaba.wisp.engine.WispPoller").getDeclaredField("fd2TaskLow");
        f.setAccessible(true);
        WispTask[] fd2TaskLow = (WispTask[]) f.get(WispPoller.INSTANCE);
        int fd = ((SelChImpl) ch).getFDVal();
        assertTrue(fd2TaskLow[fd] != null);

        System.out.println(fd2TaskLow[fd].getName());

        access.park(-1);

        assertTrue(fd2TaskLow[fd] == null);

        so.close();
    }
}
