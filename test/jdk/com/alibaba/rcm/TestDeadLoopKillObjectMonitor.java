/*
 * @test
 * @library /lib/testlibrary
 * @build TestDeadLoopKillObjectMonitor RcmUtils
 * @summary test RCM TestKillThreads
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @modules java.base/com.alibaba.rcm.internal:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -XX:+Wisp2ThreadStop -XX:ActiveProcessorCount=1 TestDeadLoopKillObjectMonitor
 */

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceType;
import com.alibaba.rcm.internal.RCMUnsafe;
import com.alibaba.wisp.engine.WispResourceContainerFactory;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestDeadLoopKillObjectMonitor {

    private static Object obj = new Object();

    public static void main(String[] args) throws Exception {
        ResourceContainer container = RcmUtils.createContainer(Collections.singletonList(
                ResourceType.CPU_PERCENT.newConstraint(100)));

        CountDownLatch latch = new CountDownLatch(1);
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (obj) {
                    try {
                        Thread.sleep(1000);
                        Thread t2 =  new Thread(new Runnable() {
                            @Override
                            public void run() {
                                latch.countDown();
                            }
                        });
                        t2.setDaemon(true);
                        t2.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
         t1.setDaemon(true);
         t1.start();

         Thread.sleep(100);
        container.run(() -> {
           Thread t = new Thread(new Runnable() {
               @Override
               public void run() {
                   while (true)
                   synchronized (obj) {
                       System.out.println("rr");
                   }
               }
           });
           t.start();
        });

        Thread.sleep(100);
        RCMUnsafe.killThreads(container);
    }

}
