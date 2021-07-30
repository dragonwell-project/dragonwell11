/*
 * @test
 * @library /lib/testlibrary
 * @summary Test Daemon Thread Group implementation
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.version=2 DaemonThreadGroupTest
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import com.alibaba.wisp.engine.WispPoller;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;


/**
 * test the Daemon Thread Group implementation
 */
public class DaemonThreadGroupTest {
    public static void main(String... arg) throws Exception {

        WispEngineAccess access = SharedSecrets.getWispEngineAccess();
        access.eventLoop();

        AtomicReference<WispTask> task1 = new AtomicReference<>();
        AtomicBoolean doUnpark = new AtomicBoolean(false);
        AtomicBoolean hasError = new AtomicBoolean(false);

        Field f = Class.forName("com.alibaba.wisp.engine.WispPoller").getDeclaredField("thread");
        f.setAccessible(true);
        Thread t = (Thread) f.get(WispPoller.INSTANCE);

        f = Class.forName("com.alibaba.wisp.engine.WispEngine").getDeclaredField("daemonThreadGroup");
        f.setAccessible(true);
        ThreadGroup threadGroup = (ThreadGroup) f.get(null);

        System.out.println(threadGroup.getName());

        assertTrue(t.getThreadGroup() == threadGroup, "the thread isn't in daemonThreadGroup");

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
