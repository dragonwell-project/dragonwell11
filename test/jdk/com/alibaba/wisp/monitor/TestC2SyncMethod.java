/*
 * @test
 * @summary test to run a compiled/synchronized method with wisp enabled.
 * @requires os.family == "linux"
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestC2SyncMethod
*/
public class TestC2SyncMethod {
    public synchronized static void main(String[] args) {
        for (int i = 0; i < 1000000; i++) {
            foo();
        }
    }

    static volatile int n = 0;

    synchronized static void foo() {
        n++;
    }
}
