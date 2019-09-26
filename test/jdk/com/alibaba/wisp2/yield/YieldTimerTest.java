/*
 * @test
 * @library /lib/testlibrary
 * @summary Verify timer could be processed when we're yielding
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm  -XX:+UseWisp2 -Dcom.alibaba.wisp.carrierEngines=1 YieldTimerTest
 */


import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class YieldTimerTest {
    public static void main(String[] args) throws Exception {
        Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        new Thread(() -> {
            while (true) {
                Thread.yield();
            }
        }).start();

        future.get(1, TimeUnit.SECONDS);
    }
}
