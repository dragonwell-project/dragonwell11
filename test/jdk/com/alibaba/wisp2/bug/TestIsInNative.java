/*
 * @test
 * @library /lib/testlibrary
 * @summary test Thread.isInNative() is correct
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine TestIsInNative
 */

import jdk.internal.misc.SharedSecrets;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;

import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestIsInNative {

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
        Thread nthread = new Thread(() -> {
            try {
                SocketChannel ch = SocketChannel.open(new InetSocketAddress(socketAddr, 80));
                ch.read(ByteBuffer.allocate(4096));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        nthread.start();
        Thread thread = new Thread(() -> {
            while (true) {

            }
        });
        thread.start();
        Thread thread2 = new Thread(() -> {
        });
        thread2.start();
        Thread.sleep(500);
        assertFalse(SharedSecrets.getJavaLangAccess().isInSameNative(nthread));
        assertFalse(SharedSecrets.getJavaLangAccess().isInSameNative(thread));
        assertFalse(SharedSecrets.getJavaLangAccess().isInSameNative(thread2));
        Thread.sleep(1000);
        assertFalse(SharedSecrets.getJavaLangAccess().isInSameNative(thread));
    }
}
