/*
 * @test
 * @library /lib/testlibrary
 * @summary Verify the context class loader isolation per co-routine
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestCtxClassLoaderIsolate
*/


import com.alibaba.wisp.engine.WispEngine;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestCtxClassLoaderIsolate {
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
