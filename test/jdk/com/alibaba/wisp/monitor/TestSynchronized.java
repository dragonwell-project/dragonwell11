/*
 * @test
 * @summary Basic test for java primitive lock(synchronized)
 * @requires os.family == "linux"
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestSynchronized
*/

import com.alibaba.wisp.engine.WispEngine;

public class TestSynchronized {
    public static void main(String[] args) {

        TestSynchronized s = new TestSynchronized();
        WispEngine.dispatch(s::foo);
        WispEngine.dispatch(s::bar);
    }

    private synchronized void foo() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void bar() {
    }
}
