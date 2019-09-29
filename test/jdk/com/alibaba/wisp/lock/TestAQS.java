/*
 * @test
 * @summary Test AQS: CountDownLatch is implement by AQS
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestAQS
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestAQS
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.CountDownLatch;

public class TestAQS {
    static CountDownLatch cd = new CountDownLatch(1);
    static CountDownLatch cd2 = new CountDownLatch(1);
    public static void main(String[] args) {
        WispEngine.dispatch(() -> {
            long start = System.currentTimeMillis();
            try {
                cd.await();
                assertInterval(start, 100, 5);
            } catch (InterruptedException e) {
                throw new Error();
            }
        });
        WispEngine.dispatch(() -> {
            long start = System.currentTimeMillis();
            try {
                cd2.await();
                assertInterval(start, 200, 5);
            } catch (InterruptedException e) {
                throw new Error();
            }
        });


        SharedSecrets.getWispEngineAccess().sleep(100);
        cd.countDown();
        SharedSecrets.getWispEngineAccess().sleep(100);
        cd2.countDown();
        SharedSecrets.getWispEngineAccess().sleep(5);
    }

    public static void assertInterval(long start, int diff, int bias) {
        if (Math.abs(System.currentTimeMillis() - start - diff) > bias)
            throw new Error("not wakeup expected");
    }
}
