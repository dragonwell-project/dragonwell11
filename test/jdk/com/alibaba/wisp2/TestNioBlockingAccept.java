/*
 * @test
 * @library /lib/testlibrary
 * @summary test nio blocking accept
 * @run main/othervm -Dcom.alibaba.wisp.carrierEngines=1 -XX:ActiveProcessorCount=1 -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.version=2 -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.allThreadAsWisp=true TestNioBlockingAccept
 */

import com.alibaba.wisp.engine.WispEngine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestNioBlockingAccept {
    public static void main(String[] args) throws Exception {
        WispEngine.dispatch(() -> {
            try {
                final ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.bind(new InetSocketAddress(0));
                ssc.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        final CountDownLatch latch = new CountDownLatch(1);
        WispEngine.dispatch(latch::countDown);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
