/*
 * @test
 * @summary test obj.wait() could be interrupted
 * @library /test/lib
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true InterruptedWaitTest
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 InterruptedWaitTest
 */

import java.util.concurrent.atomic.AtomicBoolean;

import static jdk.test.lib.Asserts.*;

public class InterruptedWaitTest {
    public static void main(String[] args) throws Exception {
        AtomicBoolean interrupted = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            synchronized (args) {
                try {
                    args.wait();
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            }
        });
        t.start();
        Thread.sleep(200L);
        t.interrupt();
        Thread.sleep(200L);


        assertTrue(interrupted.get());
    }
}

