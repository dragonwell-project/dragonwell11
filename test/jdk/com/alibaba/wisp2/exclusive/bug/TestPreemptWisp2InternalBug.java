/*
 * @test
 * @summary Verify wisp internal logic can not be preempted
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @run main TestPreemptWisp2InternalBug
 */

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.HashMap;
import java.util.concurrent.FutureTask;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;


public class TestPreemptWisp2InternalBug {
	private static String[] tasks = new String[] {"toString", "addTimer"};

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
	        for (int i = 0; i < tasks.length; i++) {
		        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
		        		"-XX:+UnlockExperimentalVMOptions",
				        "-XX:+UseWisp2", "-XX:+UnlockDiagnosticVMOptions", "-XX:+VerboseWisp", "-XX:-Inline",
                        "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
				        TestPreemptWisp2InternalBug.class.getName(), tasks[i]);
		        OutputAnalyzer output = new OutputAnalyzer(pb.start());
		        output.shouldContain("[WISP] preempt was blocked, because wisp internal method on the stack");
	        }
            return;
        }

        FutureTask future = getTask(args[0]);
        WispEngine.dispatch(future);
        future.get();
    }

	private static FutureTask toStringTask() {
		return new FutureTask<>(() -> {
			long start = System.currentTimeMillis();
			Object task = SharedSecrets.getWispEngineAccess().getCurrentTask();
			while (System.currentTimeMillis() - start < 1000) {
				for (int i = 0; i < 100; i++) {
                    task.toString();
				}
			}
			return null;
		});
	}

	private static FutureTask addTimerTask() {
		return new FutureTask<>(() -> {
			long start = System.currentTimeMillis();
			while (System.currentTimeMillis() - start < 2000) {
				for (int i = 0; i < 100; i++) {
					SharedSecrets.getWispEngineAccess().addTimer(System.nanoTime() + 1008611);
                    SharedSecrets.getWispEngineAccess().cancelTimer();
				}
			}
			return null;
		});
	}

	private static FutureTask getTask(String taskName) {
        switch (taskName) {
	            case "toString":
		        return toStringTask();
                    case "addTimer":
		        return addTimerTask();
	            default:
		        throw new IllegalArgumentException();
	    }
	}
}
