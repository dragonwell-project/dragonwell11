/*
 * @test
 * @library /lib/testlibrary
 * @summary test blocking accept
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -Dcom.alibaba.wisp.carrierEngines=1 -XX:+UseWisp2 TestBlockingAccept2
 */

import com.alibaba.wisp.engine.WispEngine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestBlockingAccept2 {
    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                latch.await(1, TimeUnit.SECONDS);
                Thread.sleep(200);
                Socket s = new Socket();
                s.connect(new InetSocketAddress(12388));
                latch2.await(1, TimeUnit.SECONDS);
                s.close();
                Thread.sleep(200);
                s = new Socket();
                s.connect(new InetSocketAddress(12388));
            } catch (Exception e) {
            }
        });
        t.start();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(12388));
        latch.countDown();
        ssc.accept();
        latch2.countDown();
        ssc.accept(); 
    }
}
