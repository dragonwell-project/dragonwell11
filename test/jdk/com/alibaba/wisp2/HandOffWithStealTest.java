/*
 * @test
 * @library /lib/testlibrary
 * @summary test long running or blocking syscall task could be retaken
 * @run main/othervm -Dcom.alibaba.wisp.carrierEngines=2 -XX:+UseWisp2 -Dcom.alibaba.wisp.enableHandOff=true -Dcom.alibaba.wisp.sysmonTickUs=100000 -Dcom.alibaba.wisp.handoffPolicy=ADAPTIVE HandOffWithStealTest
 */

import com.alibaba.wisp.engine.WispEngine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static jdk.testlibrary.Asserts.assertTrue;

public class HandOffWithStealTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch cl = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            WispEngine.dispatch(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cl.countDown();
            });
        }

        AtomicBoolean blockingFinish = new AtomicBoolean(false);

        WispEngine.dispatch(() -> {
            try {
                String[] cmdA = { "/bin/sh", "-c", " sleep 200"};
                Process process = Runtime.getRuntime().exec(cmdA);
                process.waitFor();
                blockingFinish.set(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        cl.await();
        System.out.println("await");
        assertTrue(!blockingFinish.get());
    }
}
