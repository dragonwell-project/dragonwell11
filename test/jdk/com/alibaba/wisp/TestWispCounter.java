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
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm/timeout=2000 -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.config=/tmp/wisp.config TestWispCounter
*/
public class TestWispCounter {

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
        Thread.sleep(1_000L);
        System.out.println(mbean.getSwitchCount());
        assertTrue(mbean.getSwitchCount().stream().mapToLong(Long::longValue).sum() >= taskTotal);
        System.out.println(mbean.getSelectableIOCount());
        assertTrue(mbean.getSwitchCount().stream().mapToLong(Long::longValue).sum() > 0);
        System.out.println(mbean.getParkCount());
        assertTrue(mbean.getParkCount().stream().mapToLong(Long::longValue).sum() > 0);
    }

    private static ServerSocket ss;
    private static final int BUFFER_SIZE = 1024;

    private static void startNetServer() throws IOException {
        ss = new ServerSocket();
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
            Socket so = new Socket("localhost", ss.getLocalPort());
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
