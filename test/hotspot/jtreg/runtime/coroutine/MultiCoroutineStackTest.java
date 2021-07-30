/*
 * @test
 * @library /test/lib
 * @summary Test jstack coroutine output
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.transparentAsync=true -XX:+UseWispMonitor MultiCoroutineStackTest
 */

import com.alibaba.wisp.engine.WispEngine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static jdk.test.lib.Asserts.*;

public class MultiCoroutineStackTest {
    public static void main(String[] args) throws Exception {
        baseTest();
        testCoroutineName();
        testParkingObj();
        testWaitingToLock();
    }

    private static void testCoroutineName() throws Exception {
        final String NAME = "test-coroutine-name";
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        WispEngine.dispatch(() -> {
            Thread.currentThread().setName(NAME);
            lock.lock(); // block until outter call unlock()
            lock.unlock();
        });
        boolean success = false;
        for (String line : jstack()) {
            if (line.contains("- Coroutine [") && line.contains(NAME)) {
                success = true;
                break;
            }
        }
        assertTrue(success, "coroutine name not found");
        lock.unlock();
    }

    private static void testParkingObj() throws Exception {
        Thread[] coro = new Thread[1];

        WispEngine.dispatch(() -> {
            coro[0] = Thread.currentThread();
            LockSupport.park(new MultiCoroutineStackTest());
        });

        boolean success = false;
        for (String line : jstack()) {
            if (line.contains("parking to wait for") && line.contains(MultiCoroutineStackTest.class.getSimpleName())) {
                success = true;
                break;
            }
        }
        assertTrue(success, "\"parking to wait for\" not found");
        LockSupport.unpark(coro[0]);
    }

    private static void testWaitingToLock() throws Exception {
        Object lock = new MultiCoroutineStackTest();

        synchronized (lock) {
            WispEngine.dispatch(() -> {
                synchronized (lock) {
                }
            });
            boolean success = false;
            for (String line : jstack()) {
                if (line.contains("waiting to lock") && line.contains(MultiCoroutineStackTest.class.getSimpleName())) {
                    success = true;
                    break;
                }
            }
            assertTrue(success, "\"waiting to lock\" not found");
        }
    }

    private static void baseTest() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        WispEngine.dispatch(() -> {
            Object o = new Object();
            synchronized (o) {
                lock.lock(); // block until outter call unlock()
                lock.unlock();
            }
        });

        List<String> result = jstack();

        int i = 0;
        for (; i < result.size(); i++) {
            if (result.get(i).contains("- Coroutine ["))
                break;
        }
        assertTrue(i != result.size(), "coroutine stack not found");
        assertTrue(result.get(i + 1).contains("java.dyn.CoroutineSupport.symmetricYieldTo"),
                "unexpected stack top :" + result.get(i + 1));

        boolean lockFound = false, jucFound = false;
        for (String line : result) {
            if (line.contains("java.util.concurrent.locks")) {
                jucFound = true;
            } else if (line.contains("- locked <0x")) {
                lockFound = true;
            }
        }
        assertTrue(lockFound, "synchronized not found");
        assertTrue(jucFound, "j.u.c not found");

        lock.unlock();
    }

    private static List<String> jstack() throws Exception {
        List<String> statusLines = Files.readAllLines(Paths.get("/proc/self/status"));
        String pidLine = statusLines.stream().filter(l -> l.startsWith("Pid:")).findAny().orElse("1 -1");
        int pid = Integer.valueOf(pidLine.split("\\s+")[1]);

        Process p = Runtime.getRuntime().exec(System.getProperty("java.home") + "/bin/jstack " + pid);
        List<String> result = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().collect(Collectors.toList());
        System.out.println(result.stream().collect(Collectors.joining("\n")));
        return result;
    }
}
