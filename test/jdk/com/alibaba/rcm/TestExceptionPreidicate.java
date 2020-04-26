/*
 * @test
 * @library /lib/testlibrary
 * @build TestExceptionPreidicate RcmUtils
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @summary test RCM thread inheritance callback throw exception .
 * @run main/othervm -XX:+UseWisp2 -XX:ActiveProcessorCount=4 TestExceptionPreidicate
 */

import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceType;
import com.alibaba.rcm.internal.RCMUnsafe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestExceptionPreidicate {

    public static void main(String[] args) throws Exception {
        testException();
        testRecursive();
    }

    private static void testRecursive() throws InterruptedException {
        ResourceContainer container = RcmUtils.createContainer(ResourceType.CPU_PERCENT.newConstraint(40));
        RCMUnsafe.setResourceContainerInheritancePredicate(container, t -> new Thread().equals(t));
        CountDownLatch latch = new CountDownLatch(1);
        container.run(() -> {
            try {
                new Thread(latch::countDown).start();
            } catch (StackOverflowError e) {
                //expected
            }
        });
        assertFalse(latch.await(1, TimeUnit.SECONDS));
    }

    private static void testException() throws InterruptedException {
        ResourceContainer container = RcmUtils.createContainer(ResourceType.CPU_PERCENT.newConstraint(40));
        Object obj = null;
        CountDownLatch latch = new CountDownLatch(1);
        RCMUnsafe.setResourceContainerInheritancePredicate(container, t -> obj.hashCode() == 0);
        container.run(() -> {
                    try {
                        new Thread(latch::countDown).start();
                    } catch (Exception e) {
                        //expected
                    }
                });
        assertFalse(latch.await(1, TimeUnit.SECONDS));
    }
}
