/* @test
 * @summary unit tests for coroutine steal in concurrent situation
 * @run junit/othervm/timeout=300 -XX:+EnableCoroutine CoroutineNativeConcurrentTest
 */

import org.junit.Test;

import java.dyn.Coroutine;
import java.dyn.CoroutineBase;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class CoroutineNativeConcurrentTest {

    class Invoker {
        void testMethod() {
            try {
                Coroutine.yield();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    class Constructor {
        Constructor() {
            Coroutine.yield();
        }
    }

    public void doPrivilegeTest() {
        PrivilegedAction<Double> paaa = () -> {
            PrivilegedAction<Float> paa = () -> {
                PrivilegedAction<Integer> pa = () -> {     // lambda$0
                    Coroutine.yield();
                    return 0;
                };
                return 0f;
            };
            AccessController.doPrivileged(paa);
            return 0.0d;
        };
        AccessController.doPrivileged(paaa);
    }

    public void doInvokeTest() {
        Invoker in = new Invoker();
        try {
            Method method = Invoker.class.getDeclaredMethod("testMethod");
            method.invoke(in);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void doConstructTest() {
        try {
            Class c = Class.forName("CoroutineNativeConcurrentTest$Constructor");
            c.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void randomSteal() {
        final int THREADS = 10;
        final int CORO_PER_TH = 10;
        Coroutine[] coro = new Coroutine[THREADS * CORO_PER_TH];

        for (int i = 0; i < CORO_PER_TH; i++) {
            int idx = i;
            coro[i] = new Coroutine(() -> {
                while (true) {
                    Coroutine.yield();
                }
            });
        }

        AtomicInteger sync = new AtomicInteger();

        for (int th = 1; th < THREADS; th++) {
            int cth = th;
            Thread t = new Thread(() -> {
                for (int i = 0; i < CORO_PER_TH; i++) {
                    int idx = i;
                    coro[CORO_PER_TH * cth + i] = new Coroutine(() -> {
                        while (true) {
                            doPrivilegeTest();
                            doInvokeTest();
                            doConstructTest();
                        }
                    });
                }
                sync.incrementAndGet();
                while (sync.get() != THREADS) {}

                runRandomCoroutines(System.nanoTime(), coro);
            }, "randomSteal-" + th);
            t.setDaemon(true);
            t.start();
        }

        sync.incrementAndGet();
        while (sync.get() != THREADS) {}
    }

    private static void runRandomCoroutines(long start, Coroutine[] coro) {
        while (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(5)) {
            int n = ThreadLocalRandom.current().nextInt(coro.length);
            Coroutine co = coro[n];
            if (co.getThread() != Thread.currentThread()) {
                co.steal(false);
            }
            Coroutine.yieldTo(co);
        }
    }

    public static void main(String[] args) {
        new CoroutineNativeConcurrentTest().randomSteal();
    }
}
