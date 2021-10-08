/*
 * @test
 * @library /lib/testlibrary
 * @summary verification of work stealing really happened, also test the several WorkStealCandidates of Wisp2 in different mode: interp/c1/c2
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.schedule.stealRetry=100  -Dcom.alibaba.wisp.schedule.helpStealRetry=100 TestWisp2WorkSteal
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.schedule.stealRetry=100  -Dcom.alibaba.wisp.schedule.helpStealRetry=100  -Xcomp -client TestWisp2WorkSteal
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.schedule.stealRetry=100  -Dcom.alibaba.wisp.schedule.helpStealRetry=100  -Xcomp -server TestWisp2WorkSteal
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.schedule.stealRetry=100  -Dcom.alibaba.wisp.schedule.helpStealRetry=100  -Xint TestWisp2WorkSteal
 */

import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertNE;

public class TestWisp2WorkSteal {

    // We have several WispStealCandidates in coroutine.hpp:
    // in `class WispStealCandidate`:
    // 1. jdk.internal.reflect.NativeMethodAccessorImpl.invoke0()
    // 2. jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0()
    // 3. java.security.AccessController.doPrivilege(***)
    // 4. java.lang.Object.wait()
    // If we support other new candidates, please add them into this test.

    private static void goNatives(Object lock) {
        AccessController.doPrivileged((PrivilegedAction<Void>)() -> {   // 1. AccessController.doPrivileged() test
            try {
                lock.wait();                                            // 2. Object.wait() test
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }, AccessController.getContext());
    }

    private static class Dummy {
        public Dummy(Object lock) {
            try {
                Method m = TestWisp2WorkSteal.class.getDeclaredMethod("goNatives", Object.class);
                m.invoke(null, lock);                               // 3. Method.invoke() test
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Object lock = new Object();
        AtomicReference<Thread> t = new AtomicReference<>();
        AtomicReference<Thread> t2 = new AtomicReference<>();
        WispEngine.dispatch(() -> {
            t.set(SharedSecrets.getJavaLangAccess().currentThread0());
            synchronized (lock) {
                try {
                    Constructor<Dummy> c = Dummy.class.getConstructor(Object.class);
                    c.newInstance(lock);                                // 4. Constructor.newInstance() test
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            t2.set(SharedSecrets.getJavaLangAccess().currentThread0());
        });

        // letting a coroutine occupy the carrier
        while (true) {
            AtomicReference<Boolean> found = new AtomicReference<>(null);
            WispEngine.dispatch(() -> {
                if (SharedSecrets.getJavaLangAccess().currentThread0() == t.get()) {
                    found.set(true);
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < 1000) {
                        // occupy the carrier
                    }
                } else {
                    found.set(false);
                }
            });
            while (found.get() == null) {
            }
            if (found.get()) {
                break;
            }
        }

        synchronized (lock) {
            lock.notify();
        }

        while (t2.get() == null) {
        }

        assertNE(t.get(), t2.get());
    }
}
