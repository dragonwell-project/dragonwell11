import com.alibaba.management.WispCounterMXBean;

import javax.management.MBeanServer;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jdk.testlibrary.Asserts.assertTrue;

/* @test
 * @summary WispCounterMXBean unit test
 * @library /lib/testlibrary
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm/timeout=2000 -XX:ActiveProcessorCount=4 -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true -Dcom.alibaba.shiftThreadModel=true -Dcom.alibaba.wisp.config=/tmp/wisp.config TestWispCounter
 */

public class TestWispCounter {

    public static void main(String[] args) throws Exception {

        startNetServer();
        File f = new File("/tmp/wisp.config");
        f.deleteOnExit();
        FileWriter writer = new FileWriter(f);
        writer.write("com.alibaba.wisp.biz.manage=TestWispCounter::main\n");
        writer.close();

        // WispBizSniffer has already been loaded;
        // reload WispBizSniffer's config from file.
        Method m = Class.forName("com.alibaba.wisp.engine.WispConfiguration").getDeclaredMethod("loadBizConfig");
        m.setAccessible(true);
        m.invoke(null);

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
                synchronized (TestWispCounter.class) {
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

        System.out.println(mbean.getRunningStates());
        System.out.println(mbean.getQueueLength());
        // wait task complete
        Thread.sleep(10_000L);
        System.out.println(mbean.getSwitchCount());
        assertTrue(mbean.getSwitchCount().stream().mapToLong(Long::longValue).sum() >= taskTotal);
        System.out.println(mbean.getSelectableIOCount());
        assertTrue(mbean.getSwitchCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getParkCount());
        assertTrue(mbean.getParkCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getCreateTaskCount());
        System.out.println(mbean.getCompleteTaskCount());
        System.out.println(mbean.getEventLoopCount());
        assertTrue(mbean.getEventLoopCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getUnparkInterruptSelectorCount());
        assertTrue(mbean.getTimeOutCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getTimeOutCount());
        assertTrue(mbean.getTimeOutCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getWaitTimeTotal());
        assertTrue(mbean.getWaitTimeTotal().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getRunningTimeTotal());
        assertTrue(mbean.getRunningTimeTotal().stream().mapToLong(Long::longValue).sum() > 0);
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
