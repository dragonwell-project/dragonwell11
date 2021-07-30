/*
 * @test
 * @summary Ensure we can exit vm when -XX:+UseWispMonitor
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true WispExitTest
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 WispExitTest
*/
public class WispExitTest {
    public static void main(String[] args) throws Exception {

    }
}
