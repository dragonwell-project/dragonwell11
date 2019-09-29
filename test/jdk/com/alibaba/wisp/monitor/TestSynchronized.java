/*
 * @test
 * @summary Basic test for java primitive lock(synchronized)
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestSynchronized
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestSynchronized
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
