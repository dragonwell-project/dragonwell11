/*
 * @test
 * @summary Test WispEngine's multi-task schedule
 * @modules java.base/jdk.internal.misc
 * @requires os.family == "linux"
 * @requires os.arch != "riscv64"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true TestExecution
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableCoroutine -Dcom.alibaba.wisp.transparentWispSwitch=true  TestExecution
*/


import com.alibaba.wisp.engine.WispEngine;
import com.alibaba.wisp.engine.WispTask;
import jdk.internal.misc.SharedSecrets;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * test the coroutine execute engine
 *
 */
public class TestExecution {

    static int finishCnt = 0;
    public static void main(String[] args) {
        Callable handler = () -> {
            /**
             * transform from:
             * if ((nodeA() + nodeB()).equals("A:done\nB1:done\nB2:done\n"))
             *     throw new Error("result error");
             */
            FutureTask<String> futureA = new FutureTask<>(TestExecution::nodeA);
            FutureTask<String> futureB = new FutureTask<>(TestExecution::nodeB);
            WispEngine.dispatch(futureA);
            WispEngine.dispatch(futureB);
            String result;
            try {
                result = futureA.get() + futureB.get();
            } catch (Exception e) {
                throw new Error("exception during task execution");
            }

            if (!result.equals("A:done\nB1:done\nB2:done\n"))
                throw new Error("result error");

            finishCnt++;
            return null;
        };

        /**
         * <pre>
         * WispEngine.local().createTask(() -> {
         *     while (client = accept()) {
         *         WispEngine.local().createTask(()->handler(client), "client handler");
         *     }
         * }, "accepter");
         *
         * a web server can using a accepter {@link WispTask}  create handler for every request
         * we don't have request, create 3 handler manually
         *
         * every handler is a tree root
         *         handler         handler        handler
         *         /    \           /   \          /   \
         *        A     B          A    B         A    B
         *             / \             / \            / \
         *            B1 B2           B1 B2          B1 B2
         * </pre>
         *
         * look into a particular tree:
         * B1 and B2 is created when nodeB is executed after nodeA blocked
         * business code drive the node create ..
         *
         * then B1 block; B2 executed and block
         * then the engine block on pump about 100ms..
         *
         *
         * 3 tree execute concurrently
         *
         */
        for (int i = 0; i < 3; i++) {
            WispEngine.dispatch(() -> {
                try {
                    handler.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        WispEngine.dispatch(() -> {
            SharedSecrets.getWispEngineAccess().sleep(200);
            // the 3 tree should finish after 100ms+, but the switch need warm up, give more time..
            if (finishCnt != 3) throw new Error("not finished");
        });
        SharedSecrets.getWispEngineAccess().sleep(200);
    }

    static String nodeA() {
        // node A could also be a function call after B created
        return slowReq("A");
    }

    static String nodeB() {
        /*
          transform from:
          return slowReq("B1") + slowReq("B2");
         */
        FutureTask<String> future1 = new FutureTask<>(() -> slowReq("B1"));
        FutureTask<String> future2 = new FutureTask<>(() -> slowReq("B2"));
        WispEngine.dispatch(future1);
        WispEngine.dispatch(future2);

        try {
            return future1.get() + future2.get();
        } catch (Exception e) {
            throw new Error("exception during task execution");
        }
    }

    // mimic an IO call
    static String slowReq(String arg) {
        SharedSecrets.getWispEngineAccess().sleep(100); // only block current coroutine
        return arg + ":done\n";
    }
}
