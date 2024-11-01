/*
 * @test
 * @library /lib/testlibrary
 * @summary handle this scenario:
 *      1. task A fetch a socket S and release it.
 *      2. task B get the socket S and block on IO.
 *      3. task A exit and clean S's event, now B waiting forever...
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true  TestReleaseWispSocketAndExit
*/


import com.alibaba.wisp.engine.WispEngine;
import jdk.internal.misc.SharedSecrets;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestReleaseWispSocketAndExit {
    static byte buf[] = new byte[1000];

    public static void main(String[] args) throws Exception {

        CountDownLatch latch = new CountDownLatch(1);

        CountDownLatch listen = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);

        WispEngine.dispatch(() -> {
            try {
                ServerSocket sso = new ServerSocket(51243);
                listen.countDown();
                Socket so;
                so = sso.accept();
                InputStream is = so.getInputStream();
                OutputStream os = so.getOutputStream();
                int n;
                while ((n = is.read(buf, 0, 4)) == 4) {
                    long l = Long.valueOf(new String(buf, 0, n));
                    System.out.println("l = " + l);
                    SharedSecrets.getWispEngineAccess().sleep(l);
                    os.write(buf, 0, n);
                }
            } catch (Exception e) {
                throw new Error();
            }
            finish.countDown();
        });

        listen.await();
        Socket so = new Socket("127.0.0.1", 51243);

        WispEngine.dispatch(() -> {
            try {
                InputStream is = so.getInputStream();
                OutputStream os = so.getOutputStream();
                os.write("0000".getBytes());
                System.out.println("======");
                latch.await();
                System.out.println("======2");
            } catch (Exception e) {
                throw new Error();
            }
            finish.countDown();
        });

        WispEngine.dispatch(() -> {
            try {
                InputStream is = so.getInputStream();
                OutputStream os = so.getOutputStream();

                os.write("0100".getBytes());
                System.out.println("------");
                latch.countDown();
                is.read(buf);
                is.read(buf);// blocked
                System.out.println("------2");

                so.close();
            } catch (Exception e) {
                throw new Error();
            }
            finish.countDown();
        });

        assertTrue(finish.await(5, TimeUnit.SECONDS));
        System.out.println("passed");
    }
}
