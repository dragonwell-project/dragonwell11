/*
 * @test
 * @library /lib/testlibrary
 * @summary test long running or blocking syscall task could be retaken
 * @requires os.family == "linux"
 * @run main/othervm -Dcom.alibaba.wisp.carrierEngines=1 -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.handoffPolicy=ADAPTIVE TestHandOffWithSteal
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

public class TestHandOffWithSteal {
    public static void main(String[] args) throws Exception {
        final int N = 100;
        CountDownLatch cl = new CountDownLatch(N);
        for (int i = 0; i < N; i++) {
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

        assertTrue(cl.await(3, TimeUnit.SECONDS) && !blockingFinish.get());
    }
}

