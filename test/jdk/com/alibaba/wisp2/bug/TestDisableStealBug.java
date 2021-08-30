/*
 * @test
 * @library /lib/testlibrary
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @summary test bug of update stealEnable fail
 * @run main/othervm -XX:+UseWisp2 TestDisableStealBug
 */

import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestDisableStealBug {
    public static void main(String[] args) throws Exception {
        AtomicReference<WispTask> task = new AtomicReference<>();

        WispEngine.dispatch(() -> {
            task.set(SharedSecrets.getWispEngineAccess().getCurrentTask());
            setOrGetStealEnable(task.get(), true, false);
            try {
                Thread.sleep(10);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(2000);
        boolean stealEnable = setOrGetStealEnable(task.get(), false, false);
        assertTrue(stealEnable);
    }

    private static boolean setOrGetStealEnable(WispTask task, boolean isSet, boolean b) {
        try {
            Field resumeEntryField = task.getClass().getDeclaredField("resumeEntry");
            resumeEntryField.setAccessible(true);
            final Object resumeEntry = resumeEntryField.get(task);

            Field stealEnableField = resumeEntry.getClass().getDeclaredField("stealEnable");
            stealEnableField.setAccessible(true);
            if (isSet) {
                stealEnableField.setBoolean(resumeEntry, b);
                return b;
            } else {
                return stealEnableField.getBoolean(resumeEntry);
            }
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
