/*
 * @test
 * @summary Test get tenant state
 * @library /lib/testlibrary
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main TestState
 */

import com.alibaba.rcm.ResourceContainer;
import demo.MyResourceFactory;

import java.util.Collections;

import static jdk.testlibrary.Asserts.*;


public class TestState {
    public static void main(String[] args) {
        ResourceContainer rc = MyResourceFactory.INSTANCE.createContainer(Collections.emptyList());
        assertEQ(rc.getState(), ResourceContainer.State.RUNNING);
        rc.destroy();
        assertEQ(rc.getState(), ResourceContainer.State.DEAD);
    }
}
