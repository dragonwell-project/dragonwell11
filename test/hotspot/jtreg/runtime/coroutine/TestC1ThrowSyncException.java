/*
 * @test
 * @summary test a special wisp unpark case for C1 compiled method
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @run main/othervm  -XX:-UseBiasedLocking -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true -XX:TieredStopAtLevel=1 TestC1ThrowSyncException
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

public class TestC1ThrowSyncException {
    final static Runner[] runners = new Runner[16];
    static boolean JUC;
    static final int N = 40000;

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
        public synchronized void testThrowException() {
            int[] memory = new int[10000];
            int index = 10003;
            // trigger null check exception
            memory[index] = 5;
        }
        @Override
        public void run() {
            while (current < N) {
                try {
                    testThrowException();
                } catch (RuntimeException e) {
                    if (++current % 1000 == 0)
                        System.out.println(SharedSecrets.getJavaLangAccess().currentThread0().getName() + "\t" + current);                            
                }
            }
        }
    }
}

