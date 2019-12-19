/*
 * @test
 * @summary test sleep
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true  TestSleep
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestSleep {
    public static void main(String[] args) {
        assertTrue(IntStream.range(0, 100).parallel().allMatch(ms -> {
            long start = System.currentTimeMillis();
            FutureTask<Integer> ft = new FutureTask<>(() -> {
                Thread.sleep(ms);
                return 0;
            });
            WispEngine.dispatch(ft);
            try {
                ft.get(200, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                throw new Error(e);
            }

            long interval = System.currentTimeMillis() - start;

            if (Math.abs(interval - ms) < 100) {
                return true;
            } else {
                System.out.println("ms = " + ms);
                System.out.println("interval = " + interval);
                return false;
            }
        }));
    }
}
