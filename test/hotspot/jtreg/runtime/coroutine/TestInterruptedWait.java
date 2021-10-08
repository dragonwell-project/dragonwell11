/*
 * @test
 * @summary test obj.wait() could be interrupted
 * @requires os.family == "linux"
 * @library /test/lib
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 TestInterruptedWait
 */

import java.util.concurrent.atomic.AtomicBoolean;

import static jdk.test.lib.Asserts.*;

public class TestInterruptedWait {
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

