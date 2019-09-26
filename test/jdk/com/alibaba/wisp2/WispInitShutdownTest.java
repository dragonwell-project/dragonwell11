/*
 * @test
 * @summary WispInitShutdownTest
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 WispInitShutdownTest
 */

import com.alibaba.wisp.engine.WispEngine;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.testlibrary.Asserts.assertTrue;

public class WispInitShutdownTest {
	public static CountDownLatch shutddownLatch = new CountDownLatch(1);
	public static AtomicBoolean suc = new AtomicBoolean(false);

	public static void main(String[] args) throws Exception {
		CountDownLatch finish = new CountDownLatch(1);
		AtomicReference<WispEngine> engineAtomicReference = new AtomicReference<>();

		WispEngine.dispatch(new Runnable() {
			@Override
			public void run() {
				try {
					engineAtomicReference.set(WispEngine.current());
					Class.forName("SomeC");
					suc.set(true);
				} catch (Exception e) {
					e.printStackTrace();
					assertTrue(false);
				} finally {
					finish.countDown();
				}
			}
		});

		while (engineAtomicReference.get() == null) {}
		shutddownLatch.countDown();
		engineAtomicReference.get().shutdown();
		finish.await();
		assertTrue(suc.get());
	}
}

class SomeC {
	static {
		try {
			WispInitShutdownTest.shutddownLatch.await();
			for (int i = 0; i < 10; i++) {
				Thread.sleep(40);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}