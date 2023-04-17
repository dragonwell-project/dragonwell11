/*
 * @test
 * @library /test/lib
 * @summary test clinit wait in coroutine
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestWispClinit
 */


import com.alibaba.wisp.engine.WispEngine;
import static jdk.test.lib.Asserts.assertTrue;

public class TestWispClinit {
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


