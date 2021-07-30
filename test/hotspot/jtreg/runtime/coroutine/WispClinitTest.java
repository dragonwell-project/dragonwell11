/*
 * @test
 * @library /test/lib
 * @summary test clinit wait in coroutine
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true WispClinitTest
 */


import com.alibaba.wisp.engine.WispEngine;
import static jdk.test.lib.Asserts.assertTrue;

public class WispClinitTest {
    public static void main(String[] args) {
        WispEngine.dispatch(C::ensureInit);
        assertTrue(C.initDone);
    }
}

class C {
    static boolean initDone;

    static {
        try {
            Thread.sleep(1000);
            initDone = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void ensureInit() {
    }
}


