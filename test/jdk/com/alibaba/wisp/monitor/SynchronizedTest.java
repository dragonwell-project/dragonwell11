/*
 * @test
 * @summary Basic test for java primitive lock(synchronized)
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true SynchronizedTest
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 SynchronizedTest
*/

import com.alibaba.wisp.engine.WispEngine;

public class SynchronizedTest {
    public static void main(String[] args) {

        SynchronizedTest s = new SynchronizedTest();
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
