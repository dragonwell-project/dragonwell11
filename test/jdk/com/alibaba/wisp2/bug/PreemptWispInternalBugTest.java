/*
 * @test
 * @summary Verify wisp internal logic can not be preempted
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main PreemptWispInternalBugTest
 */

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.util.concurrent.FutureTask;

public class PreemptWispInternalBugTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    "-XX:+UseWisp2", "-XX:+UnlockDiagnosticVMOptions", "-XX:+VerboseWisp",
                    "-XX:-Inline",
                    "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                    PreemptWispInternalBugTest.class.getName(), "1");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldContain("[WISP] preempt was blocked, because wisp internal method on the stack");
            return;
        }
        long start = System.currentTimeMillis();
        FutureTask<Void> future = new FutureTask<>(() -> {
            Object task = SharedSecrets.getWispEngineAccess().getCurrentTask();
            while (System.currentTimeMillis() - start < 1000) {
                for (int i = 0; i < 100; i++) {
                    task.toString();
                }
            }
            return null;
        });
        WispEngine.dispatch(future);
        future.get();
    }
}
