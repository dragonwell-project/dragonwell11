/*
 * @test
 * @library /lib/testlibrary
 * @summary Test for thread WispTask leak
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true Id2TaskMapLeakTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 Id2TaskMapLeakTest
*/
import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static jdk.testlibrary.Asserts.assertEQ;

public class Id2TaskMapLeakTest {
    public static void main(String[] args) throws Exception {
        Field f = WispTask.class.getDeclaredField("id2Task");
        f.setAccessible(true);
        Map map = (Map) f.get(null);

        int size0 = map.size();

        AtomicInteger sizeHolder = new AtomicInteger();

        new Thread(() -> {
            WispEngine.current();
            sizeHolder.set(map.size());
        }).start();
        Thread.sleep(20); // ensure thread exit;

        assertEQ(size0 + 1, sizeHolder.get());
        assertEQ(size0, map.size());
    }
}
