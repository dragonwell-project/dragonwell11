/*
 * @test
 * @library /lib/testlibrary
 * @summary Test Daemon Thread Group implementation
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.useCarrierAsPoller=false TestDaemonThreadGroup
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;


/**
 * test the Daemon Thread Group implementation
 */
public class TestDaemonThreadGroup {
    public static void main(String... arg) throws Exception {
        Field f = Class.forName("com.alibaba.wisp.engine.WispEngine").getDeclaredField("pollerThread");
        f.setAccessible(true);
        Thread t = (Thread) f.get(null);

        f = Class.forName("com.alibaba.wisp.engine.WispEngine").getDeclaredField("daemonThreadGroup");
        f.setAccessible(true);
        ThreadGroup threadGroup = (ThreadGroup) f.get(null);

        System.out.println(threadGroup.getName());

        assertTrue(t.getThreadGroup() == threadGroup, "the thread isn't in daemonThreadGroup");
    }
}

