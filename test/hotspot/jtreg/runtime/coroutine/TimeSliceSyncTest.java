/*
 * @test
 * @library /testlibrary
 * @summary This test ensures that coroutine time slice feature won't cause hang.
 * @modules java.base/jdk.internal.misc
 * @run main/othervm  -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -XX:-Inline -XX:+EnableCoroutineTimeSlice TimeSliceSyncTest
 * @run main/othervm  -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -XX:-Inline -XX:+EnableCoroutineTimeSlice -Dcom.alibaba.wisp.version=2 TimeSliceSyncTest
 */


import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TimeSliceSyncTest {
    final static Runner[] runners = new Runner[16];
    static boolean JUC;
    static final int N = 400000;

    public static void main(String[] args) throws Exception {
        doTest();
    }

    private static void doTest() {
        for (int i = 0; i < runners.length; i++) {
            runners[i] = new Runner(i);
        }
        List<Runnable> plans = new ArrayList<>(Arrays.asList(
                () -> { // only coroutine
                    for (Runner runner1 : runners) {
                        WispEngine.dispatch(runner1);
                    }
                },
                () -> { // only thread
                    int n = 0;
                    for (Runner runner : runners) {
                        new Thread(runner, "MP-THREAD-RUNNER-" + n++).start();
                    }
                },
                () -> { //mixed
                    int n = 0;
                    for (int i = 0; i < runners.length; i += 2) {
                        final int ci = i;
                        new Thread(() -> {
                            for (int j = ci; j < ci + 2 && j < runners.length; j++) {
                                WispEngine.dispatch(runners[j]);
                            }
                        }, "MP-MIX-RUNNER-" + n++).start();
                    }
                }));
        Collections.shuffle(plans);

        plans.forEach(plan -> {
            finishLatch = new CountDownLatch(runners.length);
            current = 0;
            long start = System.currentTimeMillis();
            plan.run(); // create runners
            System.out.println("cost = " + (System.currentTimeMillis() - start) + "ms");
        });

        System.out.println("-----------");
    }


    static volatile int current = 0;
    static final Lock lock = new ReentrantLock();
    static final Condition cond = lock.newCondition();
    static CountDownLatch finishLatch;

    static class Runner implements Runnable {

        private final int ord;

        public Runner(int ord) {
            this.ord = ord;
        }
        // The printX methods are synchronized, there is a monitorexit at the end of method
        // If we perform yield before monitorexit, hang may happen.
        public synchronized void print0() {
            if ((current % 10000) == 0) {
                System.out.println("print0 " + current);
            }
        }
        public synchronized void print1() {
            if ((current % 10000) == 0) {
                System.out.println("print1 " + current);
            }
        }
        public synchronized void print2() {
            if ((current % 10000) == 0) {
                System.out.println("print2 " + current);
            }
        } 
        @Override
        public void run() {
            // 8 threads are created and execute synchronized methods.
            // We must ensure that yield do not be excetued before monitorexit 
            for (int i = 0; i < N; i++) {
                int mod = current % 5;
                if (mod == 0) {
                    print0();
                } else if (mod == 1) {
                    print1();
                } else {
                    print2();
                }
                current++;
            }
        }
    }
}

