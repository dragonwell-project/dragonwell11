/*
 * @test
 * @library /lib/testlibrary
 * @summary test bug fix of SharedSecrets and Unsafe class initializer circular dependency
 * @run main UnsafeDependencyBugTest 10
 */


import java.io.File;
import java.util.concurrent.TimeUnit;

import static jdk.testlibrary.Asserts.assertTrue;

/**
 * We need thousand times to reproduce the DEADLOCK. Don't spend too much time here..
 * We already add svt test ajdk_svt/wispTest to ensure the DEADLOCK already solved.
 */
public class UnsafeDependencyBugTest {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        int i;
        for (i = 0; System.currentTimeMillis() - start < Integer.valueOf(args[0]) * 1000; i++) {
            runLauncherWithWisp();
        }
        System.out.println("tested " +  i + " times");
    }

    private static void runLauncherWithWisp() throws Exception {
        Process p = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-XX:+EnableCoroutine")
                .redirectErrorStream(true)
                .redirectOutput(new File("/dev/null"))
                .start();

        assertTrue(p.waitFor(5, TimeUnit.SECONDS));
    }
}
