/*
 * @test
 * @summary Test switch between containers
 * @library /lib/testlibrary
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main TestSkeletalAttach
 */

import com.alibaba.rcm.ResourceContainer;
import demo.MyResourceFactory;

import java.util.Collections;

import static jdk.testlibrary.Asserts.*;


public class TestSwitchBetweenContainers {

    public static void main(String[] args) {
        ResourceContainer rc1 = MyResourceFactory.INSTANCE.createContainer(Collections.emptyList());
        ResourceContainer rc2 = MyResourceFactory.INSTANCE.createContainer(Collections.emptyList());

        boolean hasIllegalStateException = false;

        try {
            rc1.run(() -> {
                rc2.run(() -> {
                });
            });
        } catch (IllegalStateException e) {
            hasIllegalStateException = true;
        }
        assertTrue(hasIllegalStateException);
        rc1.run(() -> {
            ResourceContainer.root().run(() -> {
                rc2.run(() -> {
                });
            });
        });
    }

}
