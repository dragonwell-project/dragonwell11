/*
 * @test
 * @summary test wisp time slice preempt
 * @library /lib/testlibrary
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.carrierEngines=1 TestPreempt
 */

import com.alibaba.wisp.engine.WispEngine;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestPreempt {
    public static void main(String[] args) throws Exception {
        doTest(TestPreempt::simpleLoop, true);
        for (int i = 0; i < 15; i++) {
            doTest(() -> stackTrace(0), false);  // only test sanity: don't crash
        }
    }

    private static void doTest(Runnable r, boolean checkAssert) throws Exception {
        WispEngine.dispatch(r);
        CountDownLatch latch = new CountDownLatch(1);
        WispEngine.dispatch(latch::countDown);
        boolean success = latch.await(5, TimeUnit.SECONDS);
        if (checkAssert) {
            assertTrue(success);
        }
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

    private static void stackTrace(int i) {
        if (i == 10000) {
            int x;
            do {
                x = n;
            } while (x == n);
        } else {
            stackTrace(i + 1);
        }
    }

    // TODO: handle safepoint consumed by state switch

    volatile static int n;
}
