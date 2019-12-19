/*
 * @test
 * @summary this feature is not supported in wisp2, just check compatibility
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestDisableThreadAsWispAtRuntime
*/

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Field;

import static jdk.testlibrary.Asserts.assertEQ;

public class TestDisableThreadAsWispAtRuntime {

    private static int wisp_version;

    public static void main(String[] args) throws Exception {
        switchShift(true);
        switchShift(false);
    }

    private static void switchShift(boolean val) {
        try {
            Class<?> wispClazz = Class.forName("com.alibaba.wisp.engine.WispEngine");
            Field field = wispClazz.getDeclaredField("shiftThreadModel");
            field.setAccessible(true);
            field.setBoolean(null /*static field*/, val);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
        assertEQ(WispEngine.enableThreadAsWisp(), val);
    }

}
