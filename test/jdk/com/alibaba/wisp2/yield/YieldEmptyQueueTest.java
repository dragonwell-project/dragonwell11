/*
 * @test
 * @library /lib/testlibrary
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @summary Verify yield not really happened when queue is empty
 * @run main/othervm  -XX:+UseWisp2 YieldEmptyQueueTest
 */

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;

import static jdk.testlibrary.Asserts.assertTrue;

public class YieldEmptyQueueTest {
    public static void main(String[] args) throws Exception {
        assertTrue(Executors.newSingleThreadExecutor().submit(() -> {
            long sc = (long) new ObjAccess(WispEngine.current()).ref("counter").ref("switchCount").obj;
            Thread.yield();
            return (long) new ObjAccess(WispEngine.current()).ref("counter").ref("switchCount").obj == sc;
        }).get());
    }

    static class ObjAccess {
        Object obj;

        ObjAccess(Object obj) {
            this.obj = obj;
        }

        ObjAccess ref(String field) {
            try {
                Field f;
                try {
                    f = obj.getClass().getDeclaredField(field);
                } catch (NoSuchFieldException e) {
                    f = obj.getClass().getSuperclass().getDeclaredField(field);
                }
                f.setAccessible(true);
                return new ObjAccess(f.get(obj));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }
}
