/*
 * @test
 * @library /testlibrary
 * @summary test emit_guard_for_new in C2 will add control for load
 * @requires os.family == "linux"
 * @run main/othervm -Xcomp -XX:-TieredCompilation -Xbatch -XX:CompileOnly=TestWispEmitNewGuard.testMethod -XX:+PrintCompilation -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestWispEmitNewGuard
 */


import com.alibaba.wisp.engine.WispEngine;

public class TestWispEmitNewGuard {
    static {
        System.out.println("================ static initialize ================");
        testMethod();
        System.out.println("================ static initialize done ================");
    }

    public static void main(String[] args) throws Exception{
    }

    public static int testMethod() {
        TestWispEmitNewGuard x = new TestWispEmitNewGuard(42);
        return x.value();
    }

    private int value;
    TestWispEmitNewGuard(int value) {
        this.value = value;
    }

    int value() { return this.value; }
}
