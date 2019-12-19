import com.alibaba.management.WispCounterMXBean;

import javax.management.MBeanServer;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jdk.testlibrary.Asserts.assertTrue;

/* @test
 * @summary WispCounterMXBean unit test for Detail profile data
 * @library /lib/testlibrary
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @run main/othervm/timeout=2000 -XX:+UseWisp2 -Dcom.alibaba.wisp.config=/tmp/wisp.config -Dcom.alibaba.wisp.profile=true -Dcom.alibaba.wisp.enableProfileLog=true -Dcom.alibaba.wisp.logTimeInternalMillis=3000 TestWispDetailCounter
*/

public class TestWispDetailCounter {

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
                        Thread.sleep(1L);
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
        System.out.println(mbean.getNumberOfRunningTasks());
        // wait task complete
        Thread.sleep(10_000L);
        System.out.println(mbean.getCreateTaskCount());
        System.out.println(mbean.getCompleteTaskCount());
        System.out.println(mbean.getUnparkCount());
        assertTrue(mbean.getUnparkCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getTotalBlockingTime());
        assertTrue(mbean.getTotalBlockingTime().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getWaitSocketIOCount());
        System.out.println(mbean.getTotalWaitSocketIOTime());
        System.out.println(mbean.getEnqueueCount());
        assertTrue(mbean.getEnqueueCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getTotalEnqueueTime());
        assertTrue(mbean.getTotalEnqueueTime().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getExecutionCount());
        assertTrue(mbean.getExecutionCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getTotalExecutionTime());
        assertTrue(mbean.getTotalExecutionTime().stream().mapToLong(Long::longValue).sum() > 0);

        es.submit(() -> { try {Thread.sleep(1000);} catch (Exception e){} });
        System.out.println(mbean.getNumberOfRunningTasks());

        // check log file exist
        File file = new File("wisplog.log");
        if (!file.exists()) {
            assertTrue(false, "log file isn't generated");
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
