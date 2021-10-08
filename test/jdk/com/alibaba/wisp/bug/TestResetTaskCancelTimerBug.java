/*
 * @test
 * @library /lib/testlibrary
 * @summary test reset task doesn't cancel the current task's timer unexpectedly.
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestResetTaskCancelTimerBug
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;


public class TestResetTaskCancelTimerBug {
    public static void main(String[] args) throws Exception {
        AtomicReference<WispEngine> executor = new AtomicReference<>();
        AtomicBoolean submitDone = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            executor.set(WispEngine.current());
            while (!submitDone.get()) {}
            // 4. go sleep, cause there's a pending external WispTask create operation,
            // we should create the task fist.
            // WispEngine.runTaskInternal() -> WispTask.reset() -> WispEngine.cancelTimer()
            // will remove current task's timer, the sleep() could never be wakened.
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            latch.countDown();
        }).start();
        while (executor.get() == null) {}

        // 1. create a thread(also created WispEngine A)
        executor.get().submit(() -> {});
        // 2. request create WispTask in the WispEngine A
        submitDone.set(true);
        // 3. telling WispEngine A's it's time to sleep

        // 5. wait sleep done. If latch.countDown() is not invoked inner 10 seconds, TEST FAILURE.
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
}
