/*
 * @test
 * @library /lib/testlibrary
 * @summary Test WispEngine's DatagramSocket, InitialDirContext use dup socket to query dns.
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestDatagramSocket
*/


import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.*;

public class TestDatagramSocket {
    static CountDownLatch latch = new CountDownLatch(10);

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10; i++) {
            WispEngine.dispatch(TestDatagramSocket::foo);
        }

        latch.await(5, TimeUnit.SECONDS);
        testSetAndGetReceiveBufferSize();
        testSendAndReceive();
    }

    static public void foo() {
        try {
            InitialDirContext dirCtx = new InitialDirContext();
            System.out.println(dirCtx.getAttributes("dns:/www.tmall.com"));
        } catch (Throwable e) {
            throw new Error("query dns error");
        }
        latch.countDown();
    }

    static public void testSetAndGetReceiveBufferSize() throws Exception {
        InetAddress host = InetAddress.getByName("localhost");
        int port = 9527;
        DatagramSocket so = new DatagramSocket(port);
        so.setSoTimeout(1_000);
        System.out.println("Receive buffer size: " + so.getReceiveBufferSize());
        so.setReceiveBufferSize(1024 * 32);
        System.out.println("Receive buffer size: " + so.getReceiveBufferSize());
        assertTrue(so.getReceiveBufferSize() == 1024 * 32);
        so.close();
    }

    static public void testSendAndReceive() throws Exception {
        CountDownLatch count = new CountDownLatch(1);
        InetAddress host = InetAddress.getByName("localhost");
        int port = 9527;
        DatagramSocket so = new DatagramSocket(port);
        so.setSoTimeout(1_000);
        final int loop = 1024;
        Thread receiver = new Thread(() -> {
            int received = 0;
            try {
                byte[] buf = new byte[1024 * 32];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                count.countDown();
                for (int i = 0; i < loop; i++) {
                    received++;
                    so.receive(dp);
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                System.out.println("received packets: " + received);
                // We at least received 64 packets before timeout
                assertTrue(received > 64);
            } catch (IOException e) {
                throw new Error(e);
            }
        });
        receiver.start();
        count.await();
        for (int i = 0; i < loop; i++ ) {
            byte[] buf = new byte[1024 * 32];
            DatagramPacket dp = new DatagramPacket(buf, buf.length, host, port);
            so.send(dp);
        }
        receiver.join();
    }
}
