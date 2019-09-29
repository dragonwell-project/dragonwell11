import com.alibaba.wisp.engine.WispEngine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/* @test
 * @summary Selector rebuild test
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.transparentAsync=true -Dcom.alibaba.shiftThreadModel=true -Dcom.alibaba.globalPoller=false TestSelectorRebuild
 */

public class TestSelectorRebuild {

    public static void main(String[] args) throws Exception {

        startNetServer();
        Thread wispThread;
        CountDownLatch latch = new CountDownLatch(1);

        wispThread = new Thread(() -> {
            WispEngine.dispatch(new Runnable() {
                @Override
                public void run() {
                    doNetIO();
                    latch.countDown();
                }
            });
        }, "SelectorThread");
        wispThread.start();

        Thread.sleep(2_000L);

        for(int i = 0; i < 1000; i++) {
            wispThread.interrupt();
            Thread.sleep(1);
        }

        // wait task complete
        latch.await();
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
                    OutputStream os = cs.getOutputStream();
                    try {
                        Thread.sleep(10_000L);
                    } catch (InterruptedException e) {
                    }
                    os.write(new byte[BUFFER_SIZE]);
                    os.flush();
                    os.close();
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
            InputStream is = so.getInputStream();
            int r = is.read(new byte[BUFFER_SIZE]);
            is.close();
            so.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }
}
