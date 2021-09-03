/*
 * @test
 * @summary Test the fix that unpark might not be handled in WispThread::unpark due to due to WispEngine of main thread not properly been initialized in premain().
 *
 * @run shell testPremainWithWispMonitor.sh
 */

import com.alibaba.wisp.engine.WispEngine;

import java.lang.instrument.Instrumentation;

public class TestPremainWithWispMonitor {

    private static Object lock = new Object();

    public static void main(String[] args) throws Exception {
        // BUG: before fix, WispEngine is not registered to jvm data structure
        Thread t = new Thread(() -> {
            synchronized (lock) { // blocked by current main thread
            }
        });
        synchronized (lock) {
            t.start();
            Thread.sleep(100);
        }
        // unlock, if without fix, the handling for t's unpark will be missed in WispThread::unpark.
        // The reason here is WispEngine instance is not correctly initialized for current main thread 
        // in premain phase, which is triggered by JvmtiExport::post_vm_initialized

        t.join(100);
        if (t.isAlive()) {
            throw new Error("lost unpark");
        }
    }

    public static void premain (String agentArgs, Instrumentation instArg) {
        // init WispEngine before initializeCoroutineSupport
        WispEngine.current();
    }
}

