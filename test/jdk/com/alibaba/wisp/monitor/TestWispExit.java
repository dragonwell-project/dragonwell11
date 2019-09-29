/*
 * @test
 * @summary Ensure we can exit vm when -XX:+UseWispMonitor
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestWispExit
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestWispExit
*/
public class TestWispExit {
    public static void main(String[] args) throws Exception {

    }
}
