/*
 * @test
 * @summary test basic coroutine steal mechanism
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine BasicStealTest
 */

import java.dyn.Coroutine;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static jdk.testlibrary.Asserts.assertEQ;
import static jdk.testlibrary.Asserts.assertTrue;

public class BasicStealTest {
    public static void main(String[] args) {
        AtomicReference<Coroutine> toBeStolen = new AtomicReference<>();
        Thread main = Thread.currentThread();

        Thread t = new Thread(() -> {
            Coroutine threadCoro = Thread.currentThread().getCoroutineSupport().threadCoroutine();

            Coroutine coro = new Coroutine(() -> {
                AtomicReference<Consumer<Integer>> foo = new AtomicReference<>();
                foo.set((x) -> {
                    if (x == 10) {
                        Coroutine.yieldTo(threadCoro);
                    } else {
                        System.out.println(x + " enter " + Thread.currentThread());
                        foo.get().accept(x + 1);
                        System.out.println(x + " exit" + Thread.currentThread());
                        assertEQ(Thread.currentThread(), main);
                    }
                });
                foo.get().accept(0);
            });

            Coroutine.yieldTo(coro);
            // switch from foo()...
            toBeStolen.set(coro);
            try {
                Thread.sleep(100000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "new_thread");
        t.setDaemon(true);
        t.start();

        while (toBeStolen.get() == null) {
        }

        assertTrue(toBeStolen.get().steal(false));
        Coroutine.yieldTo(toBeStolen.get());

    }
}
