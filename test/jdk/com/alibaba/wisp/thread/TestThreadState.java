/*
 * @test
 * @summary test thread.getState() of wispTask
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 TestThreadState
 *
 */

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static jdk.testlibrary.Asserts.assertEQ;


public class TestThreadState {
    private static Object lock = new Object();
    private static AtomicBoolean exit = new AtomicBoolean(false);

    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> {
            while(!exit.get()) {

            }
        });
        assertEQ(t.getState(), Thread.State.NEW);
        t.start();
        Thread.sleep(100);
        assertEQ(t.getState(), Thread.State.RUNNABLE);
        exit.set(true);
        // wait for exit
        Thread.sleep(100);
        t.join();
        assertEQ(t.getState(), Thread.State.TERMINATED);

        t = new Thread(() -> {
           LockSupport.park(lock);
           LockSupport.parkNanos(TimeUnit.HOURS.toNanos(1));
        });
        t.start();
        Thread.sleep(100);
        assertEQ(t.getState(), Thread.State.WAITING);
        LockSupport.unpark(t);
        // wait till next timed park
        Thread.sleep(500);
        assertEQ(t.getState(), Thread.State.TIMED_WAITING);

        synchronized(lock) {
            t = new Thread(() -> {
               synchronized(lock) {
               }
            });
            t.start();
            // wait till synchronized
            Thread.sleep(200);
            assertEQ(t.getState(), Thread.State.BLOCKED);
        }
    }
}
