/*
 * @test
 * @library /lib/testlibrary
 * @summary test wisp2 getThreadInfo
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 -Dcom.alibaba.wisp.threadAsWisp.black=name:test* -Xcomp TestWispThreadInfo
 */

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import static jdk.testlibrary.Asserts.*;

public class TestWispThreadInfo{
    static final int n = 1000;
    static final Thread[] threads = new Thread[n];
    static final long[] ids = new long[n];
    public static void main(String[] args) throws Exception{
        printInfo(Thread.currentThread().getId());
        for(int i = 0; i < n; i++) {
            Thread t = new Thread(()->{
                System.out.println("start Thread ID: " + Thread.currentThread().getId());
            }, "test" + i);
            threads[i] = t;
            final long tid = t.getId();
            Thread pt = new Thread(()->{
                while(true) {
                    TestWispThreadInfo.printInfo(tid);
                }
            }, "testprint" + i);
            pt.start();
        }
        Thread t1 = new Thread(()->{
            for(Thread ti: threads) {
                ti.start();
            }
        });
        t1.start();
        t1.join();

    }

    public static void printInfo(long threadID) {
        String str = "threadID: " + threadID;
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadID);
        if (threadInfo != null) {
            str += ("Thread ID: " + threadInfo.getThreadId());
            str += ("Thread Name: " + threadInfo.getThreadName());
            str += ("Thread State: " + threadInfo.getThreadState());
            str += ("Blocked Time: " + threadInfo.getBlockedTime());
            str += ("Waited Time: " + threadInfo.getWaitedTime());
            str += ("Lock Info: " + threadInfo.getLockInfo());
            str += ("Stack Trace: ");
            for (StackTraceElement element : threadInfo.getStackTrace()) {
                str += ("\t" + element);
            }
        } else {
            str += (" cannot find");
        }
        System.out.println(str);
    }
}


