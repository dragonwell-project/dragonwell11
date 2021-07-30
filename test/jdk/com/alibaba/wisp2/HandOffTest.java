/*
 * @test
 * @library /lib/testlibrary
 * @summary test long running or blocking syscall task could be retaken
 * @run main/othervm -Dcom.alibaba.wisp.carrierEngines=1 -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.version=2 -Dcom.alibaba.wisp.enableHandOff=true -Dcom.alibaba.wisp.sysmonTickUs=100000 HandOffTest
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

public class HandOffTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch cl = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            WispEngine.dispatch(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cl.countDown();
            });
        }

        AtomicBoolean blockingFinish = new AtomicBoolean(false);

        WispEngine.dispatch(() -> {
            try {
                SocketChannel ch = SocketChannel.open(new InetSocketAddress("www.example.com", 80));
                ch.read(ByteBuffer.allocate(4096));
                blockingFinish.set(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        assertTrue(cl.await(100, TimeUnit.SECONDS));
        assertTrue(!blockingFinish.get());
    }
}
