/*
 * @test
 * @summary test use sleep in RPC senorina
 * @library /lib/testlibrary
 * @run main/othervm -XX:+EnableCoroutine -XX:+UseWispMonitor -Dcom.alibaba.wisp.transparentWispSwitch=true TestSleepRPC
 **/

import com.alibaba.wisp.engine.WispEngine;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestSleepRPC {
    public static void main(String[] args) {

        List<Future<Boolean>> rpcs = IntStream.range(0, 50).mapToObj(i -> {
            AtomicBoolean done = new AtomicBoolean(false);
            FutureTask<Boolean> ft = new FutureTask<>(() -> {
                for (int n = 0; !done.get() && n < 40; n++) {
                    Thread.sleep(5);
                } // 200 ms timeout
                return done.get();
            });
            WispEngine.dispatch(ft);
            WispEngine.dispatch(new FutureTask<>(() -> {
                Thread.sleep(new Random().nextInt(100) + 1);
                done.set(true);
                return 0;
            }));

            return ft;
        }).collect(Collectors.toList());

        assertTrue(rpcs.stream().allMatch(ft -> {
            try {
                return ft.get();
            } catch (Throwable e) {
                throw new Error(e);
            }
        }));
    }
}
