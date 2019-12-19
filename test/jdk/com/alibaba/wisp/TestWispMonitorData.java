/* @test
 * @summary WispCounterMXBean unit test for Detail profile data using the API with the specified WispEngine
 * @library /lib/testlibrary
 * @modules java.base/jdk.internal.misc
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm/timeout=2000 -XX:+UseWisp2 -Dcom.alibaba.wisp.config=/tmp/wisp.config -Dcom.alibaba.wisp.profile=true TestWispMonitorData
 */

import com.alibaba.management.WispCounterMXBean;
import com.alibaba.management.WispCounterData;
import com.alibaba.wisp.engine.WispCounter;
import com.alibaba.wisp.engine.WispEngine;

import javax.management.MBeanServer;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;
import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;

import java.lang.reflect.Field;
import static jdk.testlibrary.Asserts.assertTrue;


public class TestWispMonitorData {

    public static void main(String[] args) throws Exception {

        startNetServer();

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        WispCounterMXBean mbean = null;
        try {
            mbean = ManagementFactory.newPlatformMXBeanProxy(mbs,
                    "com.alibaba.management:type=WispCounter", WispCounterMXBean.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService es = Executors.newFixedThreadPool(20);
        int taskTotal = 40;
        for (int i = 0; i < taskTotal; i++) {
            // submit task
            es.submit(() -> {
                // do park/unpark
                synchronized (TestWispDetailCounter.class) {
                    // do sleep
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // do net IO
                doNetIO();
            });
        }
        Thread.sleep(1_000L);
       
        Class<?> clazz = Class.forName("com.alibaba.wisp.engine.WispEngine");
        Field field = clazz.getDeclaredField("carrierThreads");
        field.setAccessible(true);
        Set<Thread> set = (Set<Thread>)field.get(null);
        JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

        for (Thread thread : set) {
            WispEngine engine = JLA.getWispEngine(thread);
            WispCounterData wispdata = mbean.getWispCounterData(engine.getId());
            if (wispdata != null) {
                System.out.println("WispTask is " + engine.getId());
                System.out.println(wispdata.getCompletedTaskCount());
                System.out.println(wispdata.getTotalExecutionTime());
                System.out.println(wispdata.getExecutionCount());
                System.out.println(wispdata.getTotalEnqueueTime());
                System.out.println(wispdata.getEnqueueCount());
            }
        }
    }

    private static ServerSocket ss;
    private static final int PORT = 23000;
    private static final int BUFFER_SIZE = 1024;

    private static void startNetServer() throws IOException {
        ss = new ServerSocket(PORT);
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Socket cs = ss.accept();
                    InputStream is = cs.getInputStream();
                    int r = is.read(new byte[BUFFER_SIZE]);
                    is.close();
                    cs.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void doNetIO() {
        try {
            Socket so = new Socket("localhost", PORT);
            OutputStream os = so.getOutputStream();
            os.write(new byte[BUFFER_SIZE]);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }
}
