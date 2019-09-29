/*
 * @test
 * @summary Test fix of WispEngine block on Thread.class lock
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -XX:+UnlockExperimentalVMOptions -XX:SyncKnobs="ReportSettings=1:QMode=1" -Dcom.alibaba.wisp.transparentWispSwitch=true TestThreadLock
 * @run main/othervm -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -XX:+UnlockExperimentalVMOptions -XX:SyncKnobs="ReportSettings=1:QMode=1" -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestThreadLock
 */

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TestThreadLock {
    public static void main(String[] args) throws Exception {

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<WispEngine> es = new AtomicReference<>(null);

        synchronized (Thread.class) {
            new Thread(() -> {
                es.set(WispEngine.current());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, "ThreadIdGeneratorLockTest").start();

            Thread.sleep(100); // make sure "ThreadIdGeneratorLockTest" already sleeping

            /*
                 if ((SyncFlags & 16) == 0 && nxt == NULL && _EntryList == NULL) {
                    // Try to assume the role of responsible thread for the monitor.
                    // CONSIDER:  ST vs CAS vs { if (Responsible==null) Responsible=Self }
                     Atomic::cmpxchg_ptr (Self, &_Responsible, NULL) ;
                 }
                  ----------------
                 if (_Responsible == Self || (SyncFlags & 1)) {
                     use timeout park
                 }
                 add a waiter, let _Responsible != Self
             */
            new Thread(() -> {
                try {
                    Method m = Thread.class.getDeclaredMethod("nextThreadNum");
                    m.setAccessible(true);
                    m.invoke(null);
                } catch (ReflectiveOperationException e) {
                    throw new Error(e);
                }
            }, "waiter").start();
            Thread.sleep(100); // make sure adding waiter is done

            es.get().execute(() -> { // give an name "A"
                Thread.currentThread(); // before fix, we'll hang here
                done.countDown();
            });

            // objectMonitor::EnterI generally puts waiter to head, now _cxq is:
            // "A" (os_park) --> "A" (wisp_park) --> "waiter"
            // set QMode=1 to reverse the queue
            // _Entry_list is:
            // "waiter" --> "A" (wisp_park) --> "A" (os_park)
            Thread.sleep(100); // still hold lock 100 ms, make sure the lsat wisp blocked
        }
        // release:
        // 1. wisp unpark "waiter"
        // 2. wisp unpark "A" (wisp_park) (just after waiter ends)
        // now "ThreadIdGeneratorLockTest" Thread is on os_park, test failed!

        done.await();
    }
}
