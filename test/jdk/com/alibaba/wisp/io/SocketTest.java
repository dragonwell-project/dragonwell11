/*
 * @test
 * @summary Test WispEngine's Socket
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 SocketTest
*/

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class SocketTest {

    public static void main(String[] args) throws IOException {
        System.out.println(mkSocketPair()); // test accept() and connect()
        testData();
        testBlock();
    }

    static private void testBlock() throws IOException {
        List<Socket> sop = mkSocketPair();
        new Thread(() -> {
            try {
                Socket so = sop.get(0);
                Thread.sleep(100);
                so.getOutputStream().write(new byte[2]);

                so.close();
            } catch (Exception e) {
                throw new Error(e);
            }
        }).start();

        Socket so = sop.get(1);

        long now = System.currentTimeMillis();
        if (2 != so.getInputStream().read(new byte[10])) {
            throw new Error("read error");
        }
        if (Math.abs(System.currentTimeMillis() - now - 100) > 5)
            throw new Error("not wake as expected");
    }

    static private void testData() throws IOException {
        List<Socket> sop = mkSocketPair();

        new Thread(() -> {
            try {
                Socket so = sop.get(0);
                OutputStream os = so.getOutputStream();
                byte buf[] = new byte[4];
                ByteBuffer bb = ByteBuffer.wrap(buf);
                for (int i = 0; i < 10; i++) {
                    bb.clear();
                    bb.putInt(i);
                    os.write(buf);
                }
                so.close();
            } catch (Exception e) {
                throw new Error(e);
            }
        }).start();
        Socket so = sop.get(1);
        InputStream is = so.getInputStream();
        byte buf[] = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        for (int i = 0; true; i++) {
            bb.clear();
            if (4 != is.read(buf)) {
                if (i == 10) {
                    so.close();
                    break; // ok here
                } else {
                    throw new Error("read error");
                }
            }
            if (bb.getInt() != i)
                throw new Error("data error");
        }

    }

    static private List<Socket> mkSocketPair() throws IOException {
        ServerSocket ss = new ServerSocket(13000);
        Socket so = new Socket("localhost", 13000);
        Socket so1 = ss.accept();
        ss.close();

        return Arrays.asList(so, so1);
    }
}
