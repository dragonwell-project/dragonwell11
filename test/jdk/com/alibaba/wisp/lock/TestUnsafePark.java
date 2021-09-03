/*
 * @test
 * @library /lib/testlibrary
 * @summary Test to verify we can do proper wisp scheduling while calling on Unsafe.park()
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestUnsafePark
*/

import com.alibaba.wisp.engine.WispEngine;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static jdk.testlibrary.Asserts.assertTrue;


public class TestUnsafePark {
    public static void main(String[] args) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        AtomicLong awake = new AtomicLong();

        WispEngine.dispatch(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            awake.set(System.currentTimeMillis());
        });

        long start = System.currentTimeMillis();

        unsafe.park(false, 1);
        unsafe.park(false, TimeUnit.MILLISECONDS.toNanos(500));

        assertTrue(Math.abs(awake.get() - start - 300) < 100,
                "awake should be set before unsafe.park expired " + awake.get() + " " + start);
    }
}
