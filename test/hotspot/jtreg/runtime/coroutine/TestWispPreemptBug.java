/*
 * @test
 * @summary verify vm not crash when we're preempted frequently
 * @run main/othervm -XX:ActiveProcessorCount=1 -XX:+UseWisp2 TestWispPreemptBug
 */

import java.security.MessageDigest;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestWispPreemptBug {

    public static void main(String[] args) throws Exception {
        final int TIMEOUT = 3000;
        final long start = System.currentTimeMillis();
        final byte[] data = new Date().toString().getBytes();
        ExecutorService es = Executors.newCachedThreadPool();
        for (int i = 0; i < 2; i++) {
            es.submit(() -> {
                try {
                    while (System.currentTimeMillis() - start < TIMEOUT) {
                        MessageDigest.getInstance("md5").digest(data);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }
        Thread.sleep(TIMEOUT);
    }
}

