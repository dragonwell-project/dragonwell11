/*
 * @test
 * @summary Test skeletal implementation in AbstractResourceContainer
 * @library /lib/testlibrary
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main TestSkeletalAttach
 */

import com.alibaba.rcm.ResourceContainer;
import demo.MyResourceContainer;
import demo.MyResourceFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static jdk.testlibrary.Asserts.assertTrue;


public class TestSkeletalAttach {
    public static void main(String[] args) {
        MyResourceContainer rc = (MyResourceContainer) MyResourceFactory.INSTANCE
                .createContainer(Collections.emptyList());
        rc.run(() -> {
            assertListEQ(Collections.singletonList("attach"), rc.operations);
        });
        assertListEQ(Arrays.asList("attach", "detach"), rc.operations);

        rc.operations.clear();
        rc.run(() -> {
            rc.run(() -> {
                assertListEQ(Collections.singletonList("attach"), rc.operations);
            });
        });
        assertListEQ(Arrays.asList("attach", "detach"), rc.operations);

        rc.operations.clear();
        rc.run(() -> {
            assertListEQ(Collections.singletonList("attach"), rc.operations);
            ResourceContainer.root().run(() -> {
                assertListEQ(Arrays.asList("attach", "detach"), rc.operations);
            });
        });
        assertTrue(Arrays.asList("attach", "detach", "attach", "detach").equals(rc.operations));

        rc.operations.clear();
        rc.run(() -> {
            assertListEQ(Collections.singletonList("attach"), rc.operations);
            ResourceContainer.root().run(() -> {
                rc.run(() -> {
                    assertListEQ(Arrays.asList("attach", "detach", "attach"), rc.operations);
                });
            });
        });
        assertTrue(Arrays.asList("attach", "detach", "attach", "detach", "attach", "detach").equals(rc.operations));
    }

    private static void assertListEQ(List<String> lhs, List<String> rhs) {
        assertTrue(lhs.equals(rhs), "expect " + lhs + " equals to " + rhs);
    }
}
