/*
 * @test
 * @summary test wisp time slice preempt
 * @requires os.family == "linux"
 * @library /test/lib
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.carrierEngines=1 TestPreempt

 */

import com.alibaba.wisp.engine.WispEngine;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static jdk.test.lib.Asserts.*;


public class TestPreempt {
    public static void main(String[] args) throws Exception {
        doTest(TestPreempt::jniLoop);
        doTest(TestPreempt::voidLoop);
    }

    private static void doTest(Runnable r) throws Exception {
        WispEngine.dispatch(r);
        CountDownLatch latch = new CountDownLatch(1);
        WispEngine.dispatch(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private static void voidLoop() {
        while (true) {}
    }

    private static void jniLoop() {
        try {
            while (true) { Thread.sleep(0);}
        } catch (Exception e) {
            assertTrue(false);
        }
    }
}
