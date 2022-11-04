/*
 * @test
 * @library /lib/testlibrary
 * @summary test the fix to fd leakage when socket connect refused
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestWispSocketLeakWhenConnectRefused
 */

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestWispSocketLeakWhenConnectRefused {
    public static void main(String[] args) throws IOException {
        Socket so = new Socket();
        boolean refused = false;
        try {
            so.connect(new InetSocketAddress("127.0.0.1", 80), 5);
        } catch (ConnectException e) {
            assertTrue(so.isClosed());
            refused = true;
        }

        assertTrue(refused, "connectionRefused should been thrown");
    }
}
