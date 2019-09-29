/*
 * @test
 * @library /lib/testlibrary
 * @summary Test Object.wait/notify with coroutine
 * @run main/othervm  -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestWaitNotify
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.CountDownLatch;

import static jdk.testlibrary.Asserts.assertEQ;

public class TestWaitNotify {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1; i++) {
            TestWaitNotify s = new TestWaitNotify();
            assertEQ(s.count++, 0);
            WispEngine.dispatch(s::foo);
            WispEngine.dispatch(s::bar);
            assertEQ(s.count++, 3);
            s.latch.countDown();
            synchronized (s) {
                while (s.finishCnt < 2) {
                    s.wait();
                }
            }
        }
    }

    private int count = 0;
    private int finishCnt = 0;
    private CountDownLatch latch = new CountDownLatch(1);
    private boolean fooCond = false;

    private synchronized void foo() {
        assertEQ(count++, 1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEQ(count++, 4);
        while (!fooCond) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertEQ(count++, 6);
        finishCnt++;
        notifyAll();
    }

    private void bar() {
        assertEQ(count++, 2);
        synchronized (this) {
            assertEQ(count++, 5);
            finishCnt++;
            fooCond = true;
            notifyAll();
        }
    }
}
