/*
 * @test
 * @library /lib/testlibrary
 * @summary Test timer implementation
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine TestOverflow
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;


/**
 * test the time out implementation
 */
public class TestOverflow {
    public static void main(String... arg) throws Exception {

        WispEngineAccess access = SharedSecrets.getWispEngineAccess();

        AtomicReference<WispTask> task1 = new AtomicReference<>();
        AtomicBoolean doUnpark = new AtomicBoolean(false);
        AtomicBoolean hasError = new AtomicBoolean(false);

        WispEngine.dispatch(() -> {
            task1.set(access.getCurrentTask());
            access.park(Long.MAX_VALUE);
            // if timeout is negative(< now()), this task is selected in doSchedule
            // and park returns immediately
            hasError.set(!doUnpark.get()); // should not reach here before doing unpark
        });

        access.sleep(100); // switch task
        // let task exit
        doUnpark.set(true);
        access.unpark(task1.get());
        assertTrue(!hasError.get(), "hasError.get() should be false.");
    }
}
