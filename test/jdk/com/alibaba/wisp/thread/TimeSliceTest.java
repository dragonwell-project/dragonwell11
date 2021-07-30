/*
 * @test
 * @summary test wisp time slice
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine  -XX:+EnableCoroutineTimeSlice -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.enableHandOff=true -Dcom.alibaba.wisp.sysmonTickUs=10000 -XX:-Inline -Dcom.alibaba.wisp.version=2 TimeSliceTest
*/

import com.alibaba.wisp.engine.WispEngine;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;

public class TimeSliceTest {
    public static void main(String[] args) throws Exception {
        AtomicReference<WispEngine> engine = new AtomicReference<>();
        Thread th = new Thread(() -> {
            WispEngine.dispatch(() -> {});  // register engine
            engine.set(WispEngine.current());
            while (true) {
                foo();
            }
        }, "engine_thread");
        th.setDaemon(true);
        th.start();

        while (engine.get() == null) {
        }

        boolean[] b = new boolean[]{false};

        engine.get().execute(() -> b[0] = true);

        Thread.sleep(1000);

        assertTrue(b[0]);
    }

    static int n;

    private static void foo() {
       n = new Date().toString().hashCode();
    }
}
