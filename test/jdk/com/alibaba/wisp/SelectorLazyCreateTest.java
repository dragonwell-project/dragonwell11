/*
 * @test
 * @library /lib/testlibrary
 * @summary Test for engine.selector lazy created
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.globalPoller=false SelectorLazyCreateTest
 */

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Field;
import java.net.Socket;

import static jdk.testlibrary.Asserts.assertNotNull;
import static jdk.testlibrary.Asserts.assertNull;

public class SelectorLazyCreateTest {

    public static void main(String[] args) throws Exception {
        if (!Class.forName("com.alibaba.wisp.engine.ScheduledWispEngine").isInstance(WispEngine.current())) {
          return;
        }
        Field selField = Class.forName("com.alibaba.wisp.engine.ScheduledWispEngine").getDeclaredField("selector");
        selField.setAccessible(true);

        assertNull(selField.get(WispEngine.current()));

        new Socket("wwww.taobao.com", 80);

        assertNotNull(selField.get(WispEngine.current()));
    }
}
