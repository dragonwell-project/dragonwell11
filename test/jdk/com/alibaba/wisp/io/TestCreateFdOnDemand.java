/*
 * @test
 * @library /lib/testlibrary
 * @summary Test fix of unconnected Socket fd leak.
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestCreateFdOnDemand
*/

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

import com.alibaba.wisp.engine.WispEngine;

import static jdk.testlibrary.Asserts.assertEQ;
import static jdk.testlibrary.Asserts.assertTrue;

public class TestCreateFdOnDemand {
    public static void main(String[] args) throws Exception {

        assertEQ(countFd(), countFd());
        Socket so = new Socket();
        so.setReuseAddress(true);
        so.close();
        ServerSocket sso = new ServerSocket();
        sso.setReuseAddress(true);
        sso.close();
        DatagramSocket ds = new DatagramSocket(null);
        ds.setReuseAddress(true);
        ds.close();


        final long nfd0 = countFd();
        so = new Socket();
        assertEQ(countFd(), nfd0);
        sso = new ServerSocket();
        assertEQ(countFd(), nfd0);
        ds = new DatagramSocket(null);
        assertTrue(WispEngine.transparentWispSwitch() && countFd() == nfd0); // if -Dcom.alibaba.wisp.transparentWispSwitch=false, fail

        so.setReuseAddress(true);
        assertEQ(countFd(), nfd0 + 1);
        sso.setReuseAddress(true);
        assertEQ(countFd(), nfd0 + 2);
        ds.setReuseAddress(true);
        assertEQ(countFd(), nfd0 + 3);

        so.close();
        assertEQ(countFd(), nfd0 + 2);
        sso.close();
        assertEQ(countFd(), nfd0 + 1);
        ds.close();
        assertEQ(countFd(), nfd0);
    }


    public static long countFd() throws IOException {
        long pid = ProcessHandle.current().pid();
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec("ls -l /proc/" + pid + "/fd").getInputStream()).useDelimiter("\\n");
        long cnt = 0;

        while (s.hasNext()) {
            String str = s.next();
            System.out.println(str);
            cnt += str.contains("socket") ? 1 : 0;
        }
        return cnt;
    }
}
