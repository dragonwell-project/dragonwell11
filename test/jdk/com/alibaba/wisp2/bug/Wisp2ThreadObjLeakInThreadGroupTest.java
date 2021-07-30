/*
 * @test
 * @library /lib/testlibrary
 * @summary test bug fix of thread object leak in thread group
 * @run main/othervm -XX:+UseWisp2 Wisp2ThreadObjLeakInThreadGroupTest
 */

import java.util.concurrent.CountDownLatch;

import static jdk.testlibrary.Asserts.assertEQ;

public class Wisp2ThreadObjLeakInThreadGroupTest {
    public static void main(String[] args) throws Exception {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        int count = tg.activeCount();
        CountDownLatch c1 = new CountDownLatch(1), c2 = new CountDownLatch(1);
        Thread thread = new Thread(tg, () -> {
            c1.countDown();
            try {
                c2.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, Wisp2ThreadObjLeakInThreadGroupTest.class.getSimpleName());
        thread.start();
        c1.await(); // wait start
        assertEQ(tg.activeCount(), count + 1);
        c2.countDown(); // notify finish
        thread.join();
        assertEQ(tg.activeCount(), count);
    }
}
