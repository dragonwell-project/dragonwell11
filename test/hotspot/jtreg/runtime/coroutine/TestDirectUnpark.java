/*
 * @test
 * @library /test/lib
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @summary Test the optimization of direct unpark with Object.wait/notify
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 TestDirectUnpark
*/

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.CountDownLatch;

import static jdk.test.lib.Asserts.*;

public class TestDirectUnpark {
    public static void main(String[] args) throws Exception {
        TestDirectUnpark s = new TestDirectUnpark();
        WispEngine.dispatch(s::bar);
        long now = System.currentTimeMillis();
        synchronized (s) {
            s.latch.countDown();
            while (s.finishCnt < N) {
                s.wait();
                s.finishCnt++;
                s.notifyAll();
            }
        }
        long elapsed = System.currentTimeMillis() - now;
        if (elapsed > 3000)
            throw new Error("elapsed = " + elapsed);
    }

    static volatile int finishCnt = 0;
    private CountDownLatch latch = new CountDownLatch(1);
    static final int N = 40000;

    private void bar() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (finishCnt < N) {
            synchronized (this) {
                notifyAll();
                finishCnt++;
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
