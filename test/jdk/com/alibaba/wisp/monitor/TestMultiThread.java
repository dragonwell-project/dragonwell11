/*
 * @test
 * @library /lib/testlibrary
 * @summary Test object lock with coroutine
 * @modules java.base/jdk.internal.misc
 * @run main/othervm  -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestMultiThread
 * @run main/othervm  -XX:-UseBiasedLocking -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true  TestMultiThread
 */


import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestMultiThread {
    final static Runner[] runners = new Runner[8];
    static boolean JUC;
    static final int N = 40000;

    public static void main(String[] args) throws Exception {
        JUC = true;
        System.out.println("JUC");
        doTest();
        JUC = false;
        System.out.println("ObjectMonitor");
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
            try {
                finishLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

        private void runJUC() {
            lock.lock();
            try {
                while (current < N) {
                    while (current % runners.length != ord) {
                        try {
                            cond.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (++current % 10000 == 0) // pass the token
                        System.out.println(SharedSecrets.getJavaLangAccess().currentThread0().getName() + "\t" + current);
                    cond.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }

        private void runMonitor() {
            synchronized (lock) {
                while (current < N) {
                    while (current % runners.length != ord) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (++current % 10000 == 0) // pass the token
                        System.out.println(SharedSecrets.getJavaLangAccess().currentThread0().getName() + "\t" + current);
                    lock.notifyAll();
                }
            }
        }

        @Override
        public void run() {
            if (JUC) {
                runJUC();
            } else {
                runMonitor();
            }
            finishLatch.countDown();
        }
    }
}

