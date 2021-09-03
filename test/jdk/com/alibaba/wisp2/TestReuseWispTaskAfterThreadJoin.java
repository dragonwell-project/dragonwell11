/*
 * @test
 * @library /lib/testlibrary
 * @summary test wisp task reusing after thread.join()
 * @run main/othervm  -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.version=2 -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.allThreadAsWisp=true  TestReuseWispTaskAfterThreadJoin
 */

import com.alibaba.wisp.engine.WispEngine;

public class TestReuseWispTaskAfterThreadJoin {
    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> {


        });
        t.start();
        t.join();
        // really exited.
        // reuse the wispTask
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
