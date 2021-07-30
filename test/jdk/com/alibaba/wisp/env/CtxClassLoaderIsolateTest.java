/*
 * @test
 * @library /lib/testlibrary
 * @summary Verify the context class loader isolation per co-routine
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true CtxClassLoaderIsolateTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 CtxClassLoaderIsolateTest
*/


import com.alibaba.wisp.engine.WispEngine;

import static jdk.testlibrary.Asserts.assertTrue;

public class CtxClassLoaderIsolateTest {
    public static void main(String[] args) throws Exception {
        ClassLoader loader0 = Thread.currentThread().getContextClassLoader();
        WispEngine.dispatch(() -> Thread.currentThread().setContextClassLoader(new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return super.loadClass(name);
            }
        }));

        assertTrue(Thread.currentThread().getContextClassLoader() == loader0);
    }
}
