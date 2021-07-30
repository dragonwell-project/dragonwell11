/*
 * @test
 * @library /testlibrary
 * @summary test emit_guard_for_new in C2 will add control for load
 * @run main/othervm -Xcomp -XX:-TieredCompilation -Xbatch -XX:CompileOnly=WispEmitNewGuardTest.testMethod -XX:+PrintCompilation -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true WispEmitNewGuardTest
 */


import com.alibaba.wisp.engine.WispEngine;

public class WispEmitNewGuardTest {
    static {
        System.out.println("================ static initialize ================");
        testMethod();
        System.out.println("================ static initialize done ================");
    }

    public static void main(String[] args) throws Exception{
    }

    public static int testMethod() {
        WispEmitNewGuardTest x = new WispEmitNewGuardTest(42);
        return x.value();
    }

    private int value;
    WispEmitNewGuardTest(int value) {
        this.value = value;
    }

    int value() { return this.value; }
}
