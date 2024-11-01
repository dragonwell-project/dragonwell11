/*
 * @test
 * @library /lib/testlibrary
 * @summary test for adjusting carrier number at runtime
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.growCarrierTickUs=200000  TestAdjustCarrier
 */

import com.alibaba.wisp.engine.WispEngine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestAdjustCarrier {
	public static void main(String[] args) throws Exception{
		Thread.currentThread().setName("Wisp-Sysmon");
		Class<?> claz = Class.forName("com.alibaba.wisp.engine.WispScheduler");
		Method method = claz.getDeclaredMethod("checkAndGrowWorkers", int.class);
		method.setAccessible(true);
        ExecutorService g = WispEngine.createEngine(4, Thread::new);

		CountDownLatch latch = new CountDownLatch(100);
		CountDownLatch grow = new CountDownLatch(1);

        for (int i = 0; i < 50; i++) {
            g.execute(() -> {
                try {
                    grow.await();
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        Field scheduler = Class.forName("com.alibaba.wisp.engine.WispEngine").getDeclaredField("scheduler");
		scheduler.setAccessible(true);
		method.invoke(scheduler.get(g), 100);
		grow.countDown();

        for (int i = 0; i < 50; i++) {
            g.execute(latch::countDown);
        }

		latch.await();
		Field f = claz.getDeclaredField("workers");
		f.setAccessible(true);
		System.out.println(f.get(scheduler.get(g)).getClass().toString());
		assertTrue(((Object[]) (f.get(scheduler.get(g)))).length == 100);
		Field name = Class.forName("com.alibaba.wisp.engine.WispSysmon").getDeclaredField("WISP_SYSMON_NAME");
		name.setAccessible(true);
		assertTrue(name.get(null).equals("Wisp-Sysmon"));
	}
}
