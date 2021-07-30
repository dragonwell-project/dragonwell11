/*
 * @test
 * @summary test to turn on/off shiftThreadModel on demand
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true DisableThreadAsWispAtRuntimeTest
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 DisableThreadAsWispAtRuntimeTest
*/

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispWorkerContainer;
import jdk.internal.misc.SharedSecrets;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jdk.testlibrary.Asserts.assertEQ;

public class DisableThreadAsWispAtRuntimeTest {

    private static int wisp_version;

    public static void main(String[] args) throws Exception {
        File f = File.createTempFile("wisp-", ".config");
        System.setProperty("com.alibaba.wisp.config", f.getAbsolutePath());
        f.deleteOnExit();
        FileWriter writer = new FileWriter(f);
        writer.write("com.alibaba.wisp.biz.manage=DisableThreadAsWispAtRuntimeTest::main\n");
        writer.close();

        // WispBizSniffer has already been loaded;
        // reload WispBizSniffer's config from file.
        Method m = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredMethod("loadBizConfig");
        m.setAccessible(true);
        m.invoke(null);

        Class<?> wispClazz = Class.forName("com.alibaba.wisp.engine.WispConfiguration");
        Field field = wispClazz.getDeclaredField("WISP_VERSION");
        field.setAccessible(true);
        wisp_version = field.getInt(null);

        ExecutorService es = Executors.newFixedThreadPool(100);

        for (int i = 0; i < 1000; i++) {
            boolean on = i % 2 == 0;
            switchShift(on);
            for (int j = 0; j < 50; j++) {
                assertEQ(on, es.submit(DisableThreadAsWispAtRuntimeTest::inCoroutine).get());
            }
        }

        es.shutdown();
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

    private static boolean inCoroutine() {
        return (wisp_version == 2 || SharedSecrets.getJavaLangAccess().currentThread0()
                .getName().startsWith(WispWorkerContainer.WISP_THREAD_NAME_PREFIX))

                && !SharedSecrets.getWispEngineAccess().isThreadTask(
                SharedSecrets.getWispEngineAccess().getCurrentTask());
    }
}
