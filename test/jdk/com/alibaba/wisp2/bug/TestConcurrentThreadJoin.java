/*
 * @test
 * @library /lib/testlibrary
 * @summary ensure thread.isAlive() is false after thread.join()
 * @requires os.family == "linux"
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseWisp2 TestConcurrentThreadJoin
 */

import static jdk.testlibrary.Asserts.assertFalse;

public class TestConcurrentThreadJoin {
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < 3000) {
            Thread thread = new Thread(() -> {
            });
            thread.start();
            thread.join(1000);
            assertFalse(thread.isAlive());
        }
    }
}
