/*
 * @test
 * @summary Test default ResourceContainer is root() instead of null
 * @library /lib/testlibrary
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main TestRootNotNull
 */

import jdk.internal.misc.SharedSecrets;

import static jdk.testlibrary.Asserts.assertNotNull;

public class TestRootNotNull {
    public static void main(String[] args) {
        assertNotNull(SharedSecrets.getJavaLangAccess().getResourceContainer(Thread.currentThread()));
    }
}
