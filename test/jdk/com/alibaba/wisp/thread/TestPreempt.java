/*
 * @test
 * @summary test wisp time slice preempt
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.carrierEngines=1 -Dcom.alibaba.wisp.enableHandOff=true -Dcom.alibaba.wisp.version=2 TestPreempt

 */

import com.alibaba.wisp.engine.WispWorkerContainer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestPreempt {
    public static void main(String[] args) throws Exception {
        doTest(TestPreempt::complexLoop);
        doTest(TestPreempt::simpleLoop);
    }

    private static void doTest(Runnable r) throws Exception {
        WispWorkerContainer.INSTANCE.dispatch("test", "test", r, null);
        CountDownLatch latch = new CountDownLatch(1);
        WispWorkerContainer.INSTANCE.dispatch("test", "test", latch::countDown, null);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private static void complexLoop() {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }

        while (true) {
            md.update("welcome to the wisp world!".getBytes());
            n = md.digest()[0];
        }
    }

    private static void simpleLoop() {
        int x;
        do {
            x = n;
        } while (x == n);
    }

    // TODO: handle safepoint consumed by state switch

    volatile static int n;
}
