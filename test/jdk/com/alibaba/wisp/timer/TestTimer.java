/*
 * @test
 * @summary Test timer implement
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+EnableCoroutine TestTimer
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * test the time out implement
 */
public class TestTimer {
    public static void main(String... arg) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        WispEngine.dispatch(() -> {
            long ts = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                SharedSecrets.getWispEngineAccess().sleep(100);
                long now = System.currentTimeMillis();
                if (Math.abs(now - ts - 100) > 10)
                    throw new Error();
                ts = now;
            }
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
    }
}
