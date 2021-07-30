/*
 * @test
 * @summary Test to verify threshold setting for submitted wisp tasks.
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true SubmittedTaskLimitTest
 */

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static jdk.testlibrary.Asserts.assertGT;
import static jdk.testlibrary.Asserts.assertGTE;
import static jdk.testlibrary.Asserts.assertTrue;


public class SubmittedTaskLimitTest {
    private static final int SLEEP_TIME = 400;

    public static void main(String[] args) throws Exception {
        AtomicReference<WispEngine> engine = new AtomicReference<>();
        CountDownLatch l = new CountDownLatch(1);
        new Thread(() -> {
            engine.set(WispEngine.current());
            l.countDown();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        l.await();

        Field f = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredField("EXTERNAL_SUBMIT_THRESHOLD");
        f.setAccessible(true);
        final int SUBMIT_THRESHOLD = f.getInt(null);

        long start = System.currentTimeMillis();
        for (int i = 0; i < SUBMIT_THRESHOLD; i++) {
            engine.get().execute(() -> {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        CountDownLatch l2 = new CountDownLatch(1);
        //this task will get scheduled at least one of above submitted task terminates
        engine.get().execute(l2::countDown);
        l2.await();

        assertGTE(System.currentTimeMillis() - start, 400L);
    }
}
