/*
 * @test
 * @summary test InterruptedException was thrown by sleep()
 * @library /lib/testlibrary
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestInterruptedSleep
*/

import com.alibaba.wisp.engine.WispEngine;

import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.assertLessThan;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestInterruptedSleep {
    public static void main(String[] args) {
        Thread mainCoro = Thread.currentThread();
        WispEngine.dispatch(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            mainCoro.interrupt();
        });
        long start = System.currentTimeMillis();
        boolean ie = false;
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            ie = true;
        }
        assertLessThan((int) (System.currentTimeMillis() - start), 1000);
        assertTrue(ie);
        assertFalse(mainCoro.isInterrupted());
    }
}
