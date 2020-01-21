/*
 * @test
 * @library /lib/testlibrary
 * @modules java.base/jdk.internal.misc
 * @summary Verify park not happened for a very small interval
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm  -XX:+UseWisp2 TestYieldFewNanos
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import jdk.internal.misc.SharedSecrets;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestYieldFewNanos {
    public static void main(String[] args) throws Exception {
        assertTrue(Executors.newSingleThreadExecutor().submit(() -> {
            long pc = (long) new TestYieldEmptyQueue.ObjAccess(SharedSecrets.getJavaLangAccess().getWispTask(Thread.currentThread()))
                    .ref("carrier").ref("counter").ref("parkCount").obj;
            LockSupport.parkNanos(1);
            return (long) new TestYieldEmptyQueue.ObjAccess(SharedSecrets.getJavaLangAccess().getWispTask(Thread.currentThread()))
                    .ref("carrier").ref("counter").ref("parkCount").obj == pc;
        }).get());
    }
}
