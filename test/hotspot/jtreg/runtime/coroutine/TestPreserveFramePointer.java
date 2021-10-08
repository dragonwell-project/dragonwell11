/*
 * @test
 * @summary PreserveFramePointer for coroutine stack backtrace test
 * @requires os.family == "linux"
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 -Xcomp -XX:TieredStopAtLevel=1 -XX:+PreserveFramePointer TestPreserveFramePointer
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 -Xcomp -XX:TieredStopAtLevel=1 -XX:-PreserveFramePointer TestPreserveFramePointer
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 -Xcomp -XX:-TieredCompilation -XX:+PreserveFramePointer TestPreserveFramePointer
 * @run main/othervm -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 -Xcomp -XX:-TieredCompilation -XX:-PreserveFramePointer TestPreserveFramePointer
 */


import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.CountDownLatch;

public class TestPreserveFramePointer {

    private static Object lock = new Object();

    private static final int COROUTINE_NUM = 100;

    private static CountDownLatch cdl = new CountDownLatch(1);
    private static CountDownLatch mainLock = new CountDownLatch(COROUTINE_NUM);

    public static void main(String[] args) throws InterruptedException {

        new Thread(() -> {                    // the lock holder thread to make lock contend
            synchronized (lock) {
                cdl.countDown();              // when we hold the lock, others can go on
                try {
                    Thread.sleep(100);  // hold the lock for 100ms so others contends
                    System.gc();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (true) {}
        }).start();

        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                WispEngine.dispatch(() -> {
                    try {
                        cdl.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mainLock.countDown();
                });
            }
        }).start();

        mainLock.await();
    }

}
