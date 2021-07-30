/*
 * @test
 * @library /lib/testlibrary
 * @summary Test the fix to NPE issue caused by unexpected co-routine yielding on synchronized(lock) in SelectorProvider.provider() during initialization of WispEngine
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -XX:+UseWispMonitor SelectorInitCriticalTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -XX:+UseWispMonitor -Dcom.alibaba.wisp.version=2 SelectorInitCriticalTest
*/


import java.lang.reflect.Field;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.CountDownLatch;

public class SelectorInitCriticalTest {
    public static void main(String[] args) throws Exception {
        Field f = SelectorProvider.class.getDeclaredField("lock");
        f.setAccessible(true);
        Object selectorProviderLock = f.get(null);
        CountDownLatch latch = new CountDownLatch(1);

        Thread t = new Thread(latch::countDown);

        synchronized (selectorProviderLock) {
            t.start();
            // Holding selectorProviderLock for a while which will eventually blocks the initialization of t' WispEngine
            Thread.sleep(100);
        }

        latch.await();

    }
}
