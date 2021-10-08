/*
 * @test
 * @library /lib/testlibrary
 * @summary test Socket.getChannel returns null
 * @requires os.family == "linux"
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @requires os.family == "linux"
 * @run main SocketGetChannelTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 SocketGetChannelTest
 */

import java.net.ServerSocket;
import java.net.Socket;

import static jdk.testlibrary.Asserts.assertNull;

public class SocketGetChannelTest {
    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(0);
        Socket s1 = new Socket("localhost", ss.getLocalPort());
        Socket s2 = ss.accept();
        assertNull(ss.getChannel());
        assertNull(s1.getChannel());
        assertNull(s2.getChannel());
    }
}