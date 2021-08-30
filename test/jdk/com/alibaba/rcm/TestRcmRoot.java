/*
 * @test
 * @library /lib/testlibrary
 * @build TestRcmRoot RcmUtils
 * @summary test RCM root API.
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main/othervm -XX:+UseWisp2 -Dcom.alibaba.wisp.carrierEngines=4 TestRcmRoot
 */

import jdk.internal.misc.SharedSecrets;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.internal.AbstractResourceContainer;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jdk.testlibrary.Asserts.*;

public class TestRcmRoot {

    public static void main(String[] args) throws Exception {
        ResourceContainer rc0 = RcmUtils.createContainer();
        ResourceContainer rc1 = RcmUtils.createContainer();;

        FutureTask future0 = new FutureTask<>(() -> {
            boolean isException = false;
            try {
                ResourceContainer threadContainer = SharedSecrets.getJavaLangAccess()
                        .getResourceContainer(Thread.currentThread());
                assertTrue(threadContainer == rc0, "should own the same container as its parent thread!!!");
                rc1.run(() -> {

                });
            } catch (IllegalStateException e) {
                isException = true;
            } finally {
                assertTrue(isException, "an expected IllegalStateException should happen");
            }
            return null;
        });

        FutureTask future1 = new FutureTask<>(() -> {
            boolean isException = false;
            try {
                ResourceContainer threadContainer = SharedSecrets.getJavaLangAccess()
                        .getResourceContainer(Thread.currentThread());
                assertTrue(threadContainer == rc0, "should own the same container as its parent thread!!!");
                AbstractResourceContainer.root().run(() -> {
                    rc1.run(() -> {
                        try {
                            ResourceContainer threadContainer1 = SharedSecrets.getJavaLangAccess()
                                    .getResourceContainer(Thread.currentThread());
                            assertTrue(threadContainer1 == rc1,
                                    "should own the same container as its parent thread!!!");
                        } catch (Exception e) {
                            assertTrue(false, "unexpected reflection exception happen");
                        }
                    });
                });
            } catch (IllegalStateException e) {
                isException = true;
            } finally {
                assertFalse(isException, "an IllegalStateException should never happen");
            }
            return null;
        });
        ExecutorService es = Executors.newFixedThreadPool(4);
        es.submit(() -> {
            AbstractResourceContainer.root().run(() -> rc0.run(future0));
        });
        es.submit(() -> {
            AbstractResourceContainer.root().run(() -> rc0.run(future1));
        });
        future0.get();
        future1.get();
    }
}
