/*
 * @test
 * @summary Ensure we can exit vm when -XX:+UseWispMonitor
 * @requires os.family == "linux"
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestWispExit
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true  TestWispExit
*/
public class TestWispExit {
    public static void main(String[] args) throws Exception {

    }
}
