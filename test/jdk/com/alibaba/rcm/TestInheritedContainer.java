/*
 * @test
 * @summary Test ONLY inheritedResourceContainer is inherited from parent
 * @library /lib/testlibrary
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main TestInheritedContainer
 */

import com.alibaba.rcm.ResourceContainer;
import demo.MyResourceFactory;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.JavaLangAccess;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static jdk.testlibrary.Asserts.assertEQ;


public class TestInheritedContainer {
    public static void main(String[] args) throws Exception {
        ResourceContainer rc = MyResourceFactory.INSTANCE.createContainer(Collections.emptyList());
        JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
        rc.run(() -> {
            try {
                assertEQ(ResourceContainer.root(),
                        CompletableFuture.supplyAsync(() -> JLA.getResourceContainer(Thread.currentThread())).get());
                assertEQ(rc,
                        CompletableFuture.supplyAsync(() -> JLA.getInheritedResourceContainer(Thread.currentThread())).get());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }
}
