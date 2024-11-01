/*
 * @test
 * @library /lib/testlibrary
 * @summary verify queue length not growth infinity
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 TestDisableStealBug
 */

import com.alibaba.wisp.engine.WispGroup;
import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static jdk.testlibrary.Asserts.assertLT;

public class TestSchedulerQLBug {
    public static void main(String[] args) throws Exception {
        WispGroup g = WispGroup.createGroup(2, Thread::new);
        CountDownLatch latch = new CountDownLatch(1);
        WispGroup.execute(() -> {
            TestDisableStealBug.setOrGetStealEnable(SharedSecrets.getWispEngineAccess().getCurrentTask(), true, false);
            latch.countDown();
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await();
        for (int i = 0; i < 10; i++) { // trigger steal
            g.execute(() -> { /**/});
        }

        Thread.sleep(100);
        for (int i = 0; i < 10; i++) {
            int ql = g.submit(() -> {
                try {
                    Method m = WispEngine.class.getDeclaredMethod("getTaskQueueLength");
                    m.setAccessible(true);
                    return (int) m.invoke(WispEngine.current());
                } catch (ReflectiveOperationException e) {
                    throw new Error(e);
                }
            }).get();
            assertLT(ql, 100);
        }
    }
}
