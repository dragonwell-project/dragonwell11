/*
 * @test
 * @library /lib/testlibrary
 * @summary test close will wake up blocking wispTask
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true    TestWispSocketClose
 */

import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedChannelException;


import static jdk.testlibrary.Asserts.assertTrue;


public class TestWispSocketClose {

    class Reaper extends Thread {
        Socket s;
        int timeout;

        Reaper(Socket s, int timeout) {
            this.s = s;
            this.timeout = timeout;
        }

        public void run() {
            try {
                Thread.currentThread().sleep(timeout);
                s.close();
            } catch (Exception e) {
            }
        }
    }

    TestWispSocketClose() throws Exception {
        ServerSocket ss = new ServerSocket(0);
        ss.setSoTimeout(1000);

        InetAddress ia = InetAddress.getLocalHost();
        InetSocketAddress isa =
                new InetSocketAddress(ia, ss.getLocalPort());

        // client establishes the connection
        Socket s1 = new Socket();
        s1.connect(isa);

        // receive the connection
        Socket s2 = ss.accept();

        // schedule reaper to close the socket in 5 seconds
        Reaper r = new Reaper(s2, 5000);
        r.start();

        boolean readTimedOut = false;
        try {
            s2.getInputStream().read();
        } catch (IOException e) {
            //assertTrue (e instanceof ClosedChannelException);
        }

        s1.close();
        ss.close();

        if (readTimedOut) {
            throw new Exception("Unexpected SocketTimeoutException throw!");
        }
    }

    public static void main(String args[]) throws Exception {
        new TestWispSocketClose();
    }
}
