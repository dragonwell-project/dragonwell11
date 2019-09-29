/*
 * @test
 * @summary Test Wisp engine park / unpark
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+EnableCoroutine TestPark
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.version=2 TestPark
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

import java.util.concurrent.TimeUnit;

public class TestPark {
    public static void main(String[] args) {
        WispEngineAccess access = SharedSecrets.getWispEngineAccess();

        WispTask[] task = new WispTask[1];

        WispEngine.dispatch(() -> {
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

        });

        Thread unparkThread = new Thread() {
            @Override
            public void run() {
                access.sleep(200);
                access.unpark(task[0]);
            }
        };
        unparkThread.start();

        access.eventLoop();
    }
}
