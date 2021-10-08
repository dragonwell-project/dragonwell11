/*
 * @test
 * @summary Test the config compatibility in different wisp version
 * @library /test/lib
 * @requires os.family == "linux"
 * @run main TestConfigurationCompatibilityCheck
 */
import java.util.ArrayList;
import java.util.Arrays;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestConfigurationCompatibilityCheck {
    public static void main(String[] args) throws Exception {
        incompatibility("-Dcom.alibaba.wisp.enableThreadAsWisp=true");
        incompatibility("-Dcom.alibaba.wisp.enableThreadAsWisp=true", "-Dcom.alibaba.wisp.transparentWispSwitch=false");
        incompatibility("-Dcom.alibaba.wisp.allThreadAsWisp=true");
        incompatibility("-Dcom.alibaba.wisp.allThreadAsWisp=true", "-Dcom.alibaba.wisp.enableThreadAsWisp=false");
    }


    private static void incompatibility(String... args) throws Exception {
        ArrayList<String> list = new ArrayList<>();
        list.add("-XX:+UnlockExperimentalVMOptions");
        list.add("-XX:+EnableCoroutine");
        list.addAll(Arrays.asList(args));
        list.add("-version");
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(list.toArray(new String[0]));
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("IllegalArgumentException");
    }
}
