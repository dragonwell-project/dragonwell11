/*
 * @test
 * @library /lib/testlibrary
 * @summary verify epollArray is set for Selector.select()
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.monolithicPoll=true TestMonolithicPoll
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.monolithicPoll=false TestMonolithicPoll
 */

import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;

import java.lang.reflect.Field;
import java.nio.channels.Selector;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertEQ;

public class TestMonolithicPoll {
    public static void main(String[] args) throws Exception {
        AtomicReference<WispTask> task = new AtomicReference<>();
        Executors.newSingleThreadExecutor().submit(() -> {
            task.set(SharedSecrets.getWispEngineAccess().getCurrentTask());
            Selector.open().select();
            return null;
        });

        Thread.sleep(200);

        while (task.get() == null) {
        }

        Boolean nz = Executors.newSingleThreadExecutor().submit(() -> {
            Field arrayField = WispTask.class.getDeclaredField("epollArray");
            arrayField.setAccessible(true);
            long array = arrayField.getLong(task.get());
            return array != 0;
        }).get();

        assertEQ(nz, Boolean.getBoolean("com.alibaba.wisp.monolithicPoll"));
    }
}
