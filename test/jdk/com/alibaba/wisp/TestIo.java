/*
 * @test
 * @summary Test Wisp engine's NIO support
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestIo
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true  TestIo
*/



import jdk.internal.misc.SharedSecrets;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Properties;

public class TestIo {

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

    public static void main(String[] args) {
        String result = http();

        System.out.println(result);

        if (!result.startsWith("HTTP") ||
                !result.contains("Content-") ||
                !result.contains("Server:"))
            throw new Error("protocol error");
    }

    static String http() {
        String host = socketAddr;

        try {
            SocketChannel ch = SocketChannel.open();
            ch.configureBlocking(false);

            connect(ch, InetAddress.getByName(host).getHostAddress());
            ByteBuffer bb = ByteBuffer.allocate(1000);
            String request = "HEAD / HTTP/1.0\r\nHost:" + host + "\r\n\r\n";
            bb.put(request.getBytes());
            bb.flip();
            write(ch, bb);
            bb.clear();
            read(ch, bb);

            return new String(bb.array(), 0, bb.remaining());

        } catch (IOException e) {
            throw new Error(e);
        }
    }

    static int read(SocketChannel ch, ByteBuffer bb) throws IOException {
        int n, retry = 0;
        while ((n = ch.read(bb)) == 0) {
            if (retry++ > 3)
                throw new Error("busy loop"); // make sure we're not in a busy loop
            SharedSecrets.getWispEngineAccess().registerEvent(ch, SelectionKey.OP_READ);
            SharedSecrets.getWispEngineAccess().park(-1);
        }
        return n;
    }

    static int write(SocketChannel ch, ByteBuffer bb) throws IOException {
        int n, retry = 0;
        while ((n = ch.write(bb)) == 0) {
            if (retry++ > 3)    throw new Error("busy loop");
            SharedSecrets.getWispEngineAccess().registerEvent(ch, SelectionKey.OP_WRITE);
            SharedSecrets.getWispEngineAccess().park(-1);
        }
        return n;
    }

    static void connect(SocketChannel ch, String ip) throws IOException {
        ch.connect(new InetSocketAddress(ip, 80));
        int retry = 0;
        while (!ch.finishConnect()) {
            if (retry++ > 3)    throw new Error("busy loop");
            SharedSecrets.getWispEngineAccess().registerEvent(ch, SelectionKey.OP_CONNECT);
            SharedSecrets.getWispEngineAccess().park(-1);
        }
    }
}
