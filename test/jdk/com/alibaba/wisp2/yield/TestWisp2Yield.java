/*
 * @test
 * @library /lib/testlibrary
 * @summary Test yield in wisp2
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm  -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 -Dcom.alibaba.wisp.workerEngines=1 TestWisp2Yield
 */

import com.alibaba.wisp.engine.WispEngine;

import static jdk.testlibrary.Asserts.assertTrue;


public class TestWisp2Yield {
    public static void main(String[] args) {
        boolean success[] = new boolean[1];
        WispEngine.dispatch(() -> {
            WispEngine.dispatch(() -> {
                success[0] = true;
            });
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 100) {
            }
            Thread.yield();
            while (true) {

            }
        });

        sleep(1000);

        assertTrue(success[0]);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
