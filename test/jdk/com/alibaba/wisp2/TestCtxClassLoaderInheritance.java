/*
 * @test
 * @library /lib/testlibrary
 * @summary test context ClassLoader inherit.
 * @run main/othervm -XX:+EnableCoroutine  -Dcom.alibaba.wisp.transparentWispSwitch=true  -Dcom.alibaba.wisp.version=2 -XX:+UseWispMonitor -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.allThreadAsWisp=true TestCtxClassLoaderInheritance
 */

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertEQ;

public class TestCtxClassLoaderInheritance {
    public static void main(String[] args) throws Exception {
        ClassLoader cl = new ClassLoader() {
        };

        Thread.currentThread().setContextClassLoader(cl);
        ClassLoader[] childCtxCl = new ClassLoader[1];
        CountDownLatch done = new CountDownLatch(1);
        new Thread(() -> {
            childCtxCl[0] = Thread.currentThread().getContextClassLoader();
            done.countDown();
        }).start();

        done.await(1, TimeUnit.SECONDS);
        assertEQ(cl, childCtxCl[0]);
    }
}
