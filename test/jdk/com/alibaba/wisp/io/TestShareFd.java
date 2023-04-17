/*
 * @test
 * @library /lib/testlibrary
 * @summary test support fd use acorss coroutines
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true    TestShareFd
 */

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;


import static jdk.testlibrary.Asserts.assertTrue;

public class TestShareFd {
    private static boolean success = true;
    private static ServerSocket serverSocket = null;

    public static void main(String[] args) {
        TestShareFd shareFdTest = new TestShareFd();
        shareFdTest.testAccept();
        assert success;
    }

    void testAccept() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(6402));

            Thread t1 = new TestThread();
            Thread t2 = new TestThread();
            t1.start();
            t2.start();

            Socket s = new Socket();
            s.connect(new InetSocketAddress(6402));

            Socket s1 = new Socket();
            s1.connect(new InetSocketAddress(6402));
            t1.join();
            t2.join();

        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        assertTrue(success);
    }

    class TestThread extends Thread {
        @Override
        public void run() {
            blockOnAccept(serverSocket);
        }
    }

    static void blockOnAccept(ServerSocket serverSocket) {
        try {
            serverSocket.accept();
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
    }
}

