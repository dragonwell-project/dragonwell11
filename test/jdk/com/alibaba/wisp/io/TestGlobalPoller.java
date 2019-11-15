/*
 * @test
 * @library /lib/testlibrary
 * @summary Test for Global Poller
 * @modules java.base/jdk.internal.misc
 * @modules java.base/sun.nio.ch
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.transparentAsync=true TestGlobalPoller
*/

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;
import sun.nio.ch.SelChImpl;

import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import static jdk.testlibrary.Asserts.assertTrue;
import static jdk.testlibrary.Asserts.assertNotNull;

public class TestGlobalPoller {
    private static WispEngineAccess access = SharedSecrets.getWispEngineAccess();

    static Properties p;
    static String socketAddr;
    static {
        p = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Properties>() {
                    public Properties run() {
                        return System.getProperties();
                    }
                }
        );
        socketAddr = (String)p.get("test.wisp.socketAddress");
        if (socketAddr == null) {
            socketAddr = "www.example.com";
        }
    }

    public static void main(String[] args) throws Exception {


        Socket so = new Socket(socketAddr, 80);
        so.getOutputStream().write("NOP\n\r\n\r".getBytes());
        // now server returns the data..
        // so is readable
        // current task is interested in read event.
        SocketChannel ch = so.getChannel();
        access.registerEvent(ch, SelectionKey.OP_READ);

        Class<?> clazz = Class.forName("com.alibaba.wisp.engine.WispEventPump$Pool");
        Field pumps = clazz.getDeclaredField("pumps");
        pumps.setAccessible(true);
        Object[] a = (Object[]) pumps.get(clazz.getEnumConstants()[0]);
        WispTask[] fd2TaskLow = null;
        int fd = ((SelChImpl) ch).getFDVal();
        for (Object pump : a) {
            Field f = Class.forName("com.alibaba.wisp.engine.WispEventPump").getDeclaredField("fd2ReadTaskLow");
            f.setAccessible(true);
            WispTask[] map = (WispTask[]) f.get(pump);
            if (map[fd] != null) {
                fd2TaskLow = map;
            }
        }
        assertNotNull(fd2TaskLow);

        access.park(-1);

        assertTrue(fd2TaskLow[fd] == null);

        so.close();
    }
}
