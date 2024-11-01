/*
 * @test
 * @library /lib/testlibrary
 * @summary test thread.join()
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions  -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.version=2 -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.allThreadAsWisp=true TestThreadJoin
 */

import com.alibaba.wisp.engine.WispEngine;

public class TestThreadJoin {
    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.start();
        t.join();

        t = new Thread(() -> {

        });
        t.start();
        Thread.sleep(1000);
        WispEngine.dispatch(() -> {
            try {
                Thread.sleep(200000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(1000);
        t.join();
    }
}
