/*
 * @test
 * @summary test coroutine throw Error
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestThrowError
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 TestThrowError
*/

import com.alibaba.wisp.engine.WispEngine;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestThrowError {
    public static void main(String[] args) {
        WispEngine.dispatch(() -> {
            throw new Error();
        });

        boolean[] executed = new boolean[]{false};

        WispEngine.dispatch(() -> executed[0] = true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        assertTrue(executed[0]);
    }
}
