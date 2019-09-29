/*
 * @test
 * @summary test yield()
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestYield
 */

import com.alibaba.wisp.engine.WispEngine;

import static jdk.testlibrary.Asserts.*;

public class TestYield {
    private static int i = 0;
    public static void main(String[] args) {
        WispEngine.dispatch(() -> {
            assertEQ(i++, 0);
            Thread.yield();
            assertEQ(i++, 2);
            Thread.yield();
            assertEQ(i++, 4);
            Thread.yield();
        });
        assertEQ(i++, 1);
        Thread.yield();
        assertEQ(i++, 3);
        Thread.yield();
        assertEQ(i++, 5);
    }
}
