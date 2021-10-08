/*
 * @test
 * @summary Test unpark in JNI critical case
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @run main/othervm   -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -XX:StartFlightRecording=dumponexit=true,filename=1.jfr  TestJNICritical
 */


import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.zip.Deflater;

public class TestJNICritical {
    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(16);
        AtomicInteger id = new AtomicInteger();
        ExecutorService es = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("TestJNICritical" + id.getAndIncrement());
            return t;
        });
        IntStream.range(0, 4).forEach(ign -> es.execute(() -> {
            Deflater d = new Deflater();
            AtomicInteger n = new AtomicInteger();
            for (int i = 0; i < 4; i++) {
                WispEngine.dispatch(() -> {
                    while (n.get() < 1_000_000) {
                        jniCritical(d);
                        if (n.incrementAndGet() % 100000 == 0) {
                            System.out.println(SharedSecrets.getJavaLangAccess().currentThread0().getName() + "/" + n.get() / 1000 +"k");
                        }
                    }
                    latch.countDown();
                });
            }
        }));
        latch.await();
    }


    private static void jniCritical(Deflater d) {
        d.reset();
        d.setInput(bs);
        d.finish();
        byte[] out = new byte[4096 * 4];

        d.deflate(out); // Enter the JNI critical block here.
    }

    static byte[] bs = new byte[12];
}
