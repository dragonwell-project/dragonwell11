
package gc.z.unloading;

/*
 * @test TestUnloadFrequency
 * @requires vm.gc.Z & !vm.graal.enabled
 * @library / /test/lib /runtime/testlibrary
 * @summary Unload Frequency Bug
 * @build sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                                sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:+UseZGC -XX:ZUnloadClassesFrequency=2 -Xlog:gc* -Xms128m -Xmx128m -XX:TieredStopAtLevel=1 -XX:-Inline
 *      gc.z.unloading.TestUnloadFrequency
 */

import com.sun.management.GarbageCollectionNotificationInfo;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import static gc.testlibrary.Allocation.blackHole;

class Recorder {
    public Object fld;
}

class ZUnloadingTestCase {
    public static Object foo() {
        return bar(Boolean.TRUE); // immediate value
    }

    public static Object bar(Boolean b) {
        Recorder obj = new Recorder();
        obj.fld = b; // (critical) store an immediate value from foo()
        return obj;
    }
}

/*
In this test, we check if the nmethod entry barrier of foo() (complied into a nmethod) is armed or disarmed correctly during ZGC cycles,
especially the cycles that do not unload classes.

With -XX:ZUnloadClassesFrequency=2, odd ZGC cycles do unloading, while even ZGC cycles do not unload classes.

We let foo() be called once during the concurrent mark phase of each odd GC cycle (GC(1), GC(3), ...).
If calling foo() during GC(1) triggers its nmethod entry barrier correctly, the disarmed value of foo() should be set to Marked0.
Assume that foo()'s nmethod entry barrier was not disarmed during GC(2), we will find the disarmed value of foo() is Marked0 during GC(3),
which means calling foo() during GC(3) will not trigger its nmethod entry barrier. Therefore, the immediate values in foo() that are lastly
updated by GC(2) may be used mistakenly during GC(3).

ZGC checks each store in the interpreter mode (ZBarrierSetAssembler::store_at()). Therefore, we let foo() passes an immediate value to bar()
(not compiled), then bar() stores the value to an arbitrary object.
*/
public class TestUnloadFrequency {
    public static WhiteBox wb = WhiteBox.getWhiteBox();

    private static AtomicLong cycleId = new AtomicLong(-1);
    private static boolean neverPausedInCycle = false;

    public static void main(String[] args) throws Exception {
        // Setup
        compileOnlyFooWithoutBar();
        callFooInOddCycles(); // try to call foo() during concurrent mark of odd ZGC cycles

        // Trigger at least 20 ZGC cycles, because foo() is not always called during concurrent mark in time
        while (cycleId.get() < 20) {
            blackHole(new byte[4096]);
        }
    }

    private static void compileOnlyFooWithoutBar() throws Exception {
        Method methodFoo = ZUnloadingTestCase.class.getDeclaredMethod("foo");
        Method methodBar = ZUnloadingTestCase.class.getDeclaredMethod("bar", Boolean.class);

        wb.makeMethodNotCompilable(methodBar);
        while (!wb.isMethodCompiled(methodFoo)) {
            ZUnloadingTestCase.foo();
        }

        Asserts.assertTrue(wb.isMethodCompiled(methodFoo), "foo() should be compiled.");
        Asserts.assertFalse(wb.isMethodCompiled(methodBar), "bar() should not be compiled.");
    }

    private static void callFooInOddCycles() {
        final NotificationListener listener = (Notification notification, Object ignored) -> {
            final var type = notification.getType();
            if (!type.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                return;
            }

            final var data = (CompositeData)notification.getUserData();
            final var info = GarbageCollectionNotificationInfo.from(data);
            final var name = info.getGcName();

            if (name.equals("ZGC Cycles")) {
                final var id = info.getGcInfo().getId();
                cycleId.set(id);
                neverPausedInCycle = true;
            } else if (name.equals("ZGC Pauses")) {
                if ((cycleId.get() % 2 == 1) && neverPausedInCycle) {
                    ZUnloadingTestCase.foo();
                    System.out.println("try to call foo() during GC(" + cycleId + ") Concurrent Mark");
                    neverPausedInCycle = false;
                }
            }
        };

        for (final var collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            final NotificationEmitter emitter = (NotificationEmitter)collector;
            emitter.addNotificationListener(listener, null, null);
        }
    }
}
