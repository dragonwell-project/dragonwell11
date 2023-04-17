/*
 * @test
 * @summary test shutdown a thread pool which contains non-fully-started thread
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true  TestThreadPoolFastShutdownBug

 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestThreadPoolFastShutdownBug {
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
