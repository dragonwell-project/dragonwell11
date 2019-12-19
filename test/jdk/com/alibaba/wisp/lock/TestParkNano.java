/*
 * @test
 * @summary Test park nanos
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestParkNano
*/


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestParkNano {
    public static void main(String[] args) throws Exception {
        doTest(400_000, 300_000);
        doTest(800_000, 500_000);
    }

    private static void doTest(long wait, long expected) throws Exception {
        CountDownLatch l = new CountDownLatch(1);
        long start = System.nanoTime();
        l.await(wait, TimeUnit.NANOSECONDS);
        long diff = System.nanoTime() - start;

        if (diff < expected)
            throw new Error("wake up too early!");
    }
}
