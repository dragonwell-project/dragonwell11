/*
 * @test
 * @library /lib/testlibrary
 * @summary test thread.join()
 * @run main/othervm  -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.version=2 -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.allThreadAsWisp=true ThreadJoinTest
 */

import com.alibaba.wisp.engine.WispEngine;

public class ThreadJoinTest {
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
