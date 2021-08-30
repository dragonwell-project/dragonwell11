/*
 * @test
 * @summary test timed Jvm park
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestTimedWait
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.CountDownLatch;

public class TestTimedWait {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        for (int i = 0; i < 4; i++) {
            int id = i;
            WispEngine.dispatch(() -> {
                final Object lock = new Object();
                synchronized (lock) {
                    try {
                        lock.wait(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                latch.countDown();
            });
        }
        latch.await();
    }
}
