/*
 * @test
 * @summary test submit task to engine.
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true EngineExecutorTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 EngineExecutorTest 
*/

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EngineExecutorTest {
    public static void main(String[] args) throws Exception {
        testExecutor();
    }

    private static void testExecutor() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            WispEngine.current();
            latch.countDown();
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
        latch.await();

        Executor e = SharedSecrets.getJavaLangAccess().getWispEngine(t);
        CountDownLatch latch1 = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            e.execute(latch1::countDown);
        }

        latch1.await(1, TimeUnit.SECONDS);
    }
}
