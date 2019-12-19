/*
 * @test
 * @summary Test low level coroutine implement
 * @run main/othervm -XX:+EnableCoroutine TestCoroutine
 * @run main/othervm -XX:+EnableCoroutine  TestCoroutine
*/

import java.dyn.Coroutine;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * test low level coroutine implement
 * 2 coroutine switch to each other and see the sequence
 */
public class TestCoroutine {
    static int i = 0;
    static Coroutine co1, co2;

    public static void main(String[] args) {
        try {

            co1 = new Coroutine(() -> {
                try {
                    if (i++ != 0)
                        throw new RuntimeException("co1 wrong sequence, expect 0");
                    Coroutine.yieldTo(co2);
                    if (i++ != 2)
                        throw new RuntimeException("co1 wrong sequence, expect 2");
                    Coroutine.yieldTo(co2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            co2 = new Coroutine(() -> {

                try {
                    if (i++ != 1)
                        throw new RuntimeException("co2 wrong sequence, expect 1");
                    Coroutine.yieldTo(co1);
                    if (i++ != 3)
                        throw new RuntimeException("co2 wrong sequence, expect 3");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Coroutine.yieldTo(co1);


        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
