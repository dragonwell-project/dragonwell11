/*
 * @test
 * @summary test shutdown a thread pool which contains non-fully-started thread
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=1 ThreadPoolFastShutdownBugTest
 * @run main/othervm -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true -Dcom.alibaba.wisp.version=2 ThreadPoolFastShutdownBugTest

 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolFastShutdownBugTest {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new InitThread());
        executorService.shutdown();
    }

    static class InitThread implements Runnable {

        @Override
        public void run() {

        }
    }
}
