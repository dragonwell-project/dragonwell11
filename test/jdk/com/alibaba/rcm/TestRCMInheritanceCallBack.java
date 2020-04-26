/*
 * @test
 * @library /lib/testlibrary
 * @build TestRCMInheritanceCallBack RcmUtils
 * @summary test RCM cpu resource control.
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main/othervm -XX:+UseWisp2 -XX:ActiveProcessorCount=4 TestRCMInheritanceCallBack
 */

import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceType;
import com.alibaba.rcm.internal.RCMUnsafe;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertEQ;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestRCMInheritanceCallBack {
    public static void main(String[] args) throws Exception {
        ResourceContainer container = RcmUtils.createContainer(ResourceType.CPU_PERCENT.newConstraint(100));
        RCMUnsafe.setResourceContainerInheritancePredicate(container, thread -> thread.getName().startsWith("Tenant"));
        CountDownLatch latch = new CountDownLatch(2);

        container.run(() -> {
            Thread t = new Thread(() -> {
                assertInRoot(false);
                latch.countDown();
            }, "TenantWorker");
            t.start();
        });

        container.run(() -> {
            Thread t = new Thread(() -> {
                assertInRoot(true);
                latch.countDown();
            });
            t.start();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    private static void assertInRoot(boolean flag) {
        assertEQ(flag, ResourceContainer.current() == ResourceContainer.root());
    }
}
