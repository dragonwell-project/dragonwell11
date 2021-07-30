/*
 * @test
 * @summary test to run a compiled/synchronized method with wisp enabled.
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true C2SyncMethodTest
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 C2SyncMethodTest
*/
public class C2SyncMethodTest {
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
