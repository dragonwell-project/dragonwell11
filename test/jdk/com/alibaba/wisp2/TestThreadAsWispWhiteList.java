/*
 * @test
 * @library /lib/testlibrary
 * @summary test thread as wisp white list
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+UseWisp2 -Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.allThreadAsWisp=false -Dcom.alibaba.wisp.threadAsWisp.white=name:wisp-* TestThreadAsWispWhiteList
 */

import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.FutureTask;

import static jdk.testlibrary.Asserts.assertFalse;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestThreadAsWispWhiteList {
    public static void main(String[] args) throws Exception {
        FutureTask<Boolean> future = new FutureTask<>(ThreadAsWispWhiteListTest::isRealThread);
        new Thread(future, "wisp-1").start();
        assertFalse(future.get());
        future = new FutureTask<>(ThreadAsWispWhiteListTest::isRealThread);
        new Thread(future, "thread-1").start();
        assertTrue(future.get());
    }

    private static boolean isRealThread() {
        return SharedSecrets.getJavaLangAccess().currentThread0() == Thread.currentThread();
    }
}
