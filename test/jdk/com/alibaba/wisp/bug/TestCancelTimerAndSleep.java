/*
 * @test
 * @summary Test sleep
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestCancelTimerAndSleep
*/

import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

import java.util.concurrent.TimeUnit;

public class TestCancelTimerAndSleep {

    public static void main(String[] args) throws Exception {
        WispEngineAccess access = SharedSecrets.getWispEngineAccess();

        access.addTimer(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(10));
        access.cancelTimer();

        long now = System.currentTimeMillis();
        Thread.sleep(200);
        long elapsed = System.currentTimeMillis() - now;
        if (Math.abs(elapsed - 200) > 10)
            throw new Error("elapsed = " + elapsed);
    }
}
