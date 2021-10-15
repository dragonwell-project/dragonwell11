/*
 * @test
 * @library /lib/testlibrary
 * @summary test the fix to fd leakage when socket connect timeout
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true  TestWispSocketLeakWhenConnectTimeout
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestWispSocketLeakWhenConnectTimeout {
    public static void main(String[] args) throws IOException {
        Socket so = new Socket();
        boolean timeout = false;
        try {
            so.connect(new InetSocketAddress("www.example.com", 80), 5);
        } catch (SocketTimeoutException e) {
            assertTrue(so.isClosed());
            timeout = true;
        }

        assertTrue(timeout, "SocketTimeoutException should been thrown");
    }
}
