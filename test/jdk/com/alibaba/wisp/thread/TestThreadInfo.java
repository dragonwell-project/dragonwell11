/*
 * @test
 * @summary test threadInfo in WispThreadWrapper
 * @modules java.base/com.alibaba.wisp.engine:+open
 * @library /lib/testlibrary
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2  TestThreadInfo
 *
 */

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.locks.LockSupport;

import static jdk.testlibrary.Asserts.assertEQ;
import static jdk.testlibrary.Asserts.assertNotNull;


public class TestThreadInfo {
    public static void main(String[] args) {
        Object blocker = new Object();
        long[] ids = new long[4];
        for (int i = 0; i < ids.length; i++) {
            Thread t = new Thread(() -> LockSupport.park(blocker));
            t.setName("t" + i);
            ids[i] = t.getId();
            t.start();
        }
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = tmx.getThreadInfo(ids);
        for (int i = 0; i < infos.length; i++) {
            assertNotNull(infos[i]);
            assertEQ(infos[i].getThreadState(), Thread.State.WAITING);
            assertEQ(infos[i].getLockName(), blocker.toString());
            assertEQ(infos[i].getThreadId(), ids[i]);
            assertEQ(infos[i].getThreadName(), "t" + i);
        }
    }
}
