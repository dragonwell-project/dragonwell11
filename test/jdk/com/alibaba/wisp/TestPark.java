/*
 * @test
 * @library /lib/testlibrary
 * @summary Test Wisp engine park / unpark
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+UseWisp2 TestPark
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestPark {
    public static void main(String[] args)  throws Exception {
        WispEngineAccess access = SharedSecrets.getWispEngineAccess();

        WispTask[] task = new WispTask[1];

        FutureTask<Boolean> ft = new FutureTask<>(() -> {
            task[0] = access.getCurrentTask();
            long start, diff;

            start = System.currentTimeMillis();
            access.park(0);

            diff = System.currentTimeMillis() - start;
            if (diff < 200 || diff > 220)
                throw new Error("error test unpark by other thread");



            start = start + diff;
            access.park(TimeUnit.MILLISECONDS.toNanos(200));
            diff = System.currentTimeMillis() - start;

            if (diff < 200 || diff > 220)
                throw new Error("error test timed park");



            start = start + diff;
            access.unpark(access.getCurrentTask());
            access.park(0);
            diff = System.currentTimeMillis() - start;
            if (diff > 20)
                throw new Error("error test permitted park");
            return true;

        });
        WispEngine.dispatch(ft);

        Thread unparkThread = new Thread(() -> {
            access.sleep(200);
            access.unpark(task[0]);
        });
        unparkThread.start();

        assertTrue(ft.get(50, TimeUnit.SECONDS));
    }
}
