import jdk.crac.*;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracProcess;
import jdk.test.lib.crac.CracTest;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary test if pipe can restore
 * @library /test/lib
 * @build RestorePipeFdTest
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest
 */
public class RestorePipeFdTest implements CracTest{
    private static final String MSG_1 = "[before checkpointRestore]this is an message from stdout";
    private static final String MSG_2 = "[before checkpointRestore]this is an message from stderr";
    private static final String MSG_3 = "[after checkpointRestore]this is an message from stdout";
    private static final String MSG_4 = "[after checkpointRestore]this is an message from stderr";

    @Override
    public void test() throws Exception {
        CracBuilder cracBuilder = new CracBuilder().captureOutput(true)
                                          .restorePipeStdOutErr(true);
        CracProcess crProcess = cracBuilder.startCheckpoint();
        crProcess.waitForCheckpointed();
        OutputAnalyzer crOutputAnalyzer = crProcess.outputAnalyzer();
        crOutputAnalyzer.shouldContain(MSG_1);
        crOutputAnalyzer.shouldContain(MSG_1);

        CracProcess restoreProcess = cracBuilder.doRestore();
        OutputAnalyzer restoreOutputAnalyzer = restoreProcess.outputAnalyzer();
        restoreOutputAnalyzer.shouldContain(MSG_3);
        restoreOutputAnalyzer.shouldContain(MSG_4);
    }

    @Override
    public void exec() throws Exception {
        System.out.println(MSG_1);
        System.err.println(MSG_2);
        Core.checkpointRestore();
        System.out.println(MSG_3);
        System.err.println(MSG_4);
    }
}
