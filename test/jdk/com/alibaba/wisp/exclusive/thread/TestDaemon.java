/*
 * @test
 * @library /lib/testlibrary
 * @library /test/lib
 * @requires os.family == "linux"
 * @summary test thread as wisp still keep the daemon semantic
 *
 * @run main TestDaemon
 */

import static jdk.testlibrary.Asserts.assertEQ;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


public class TestDaemon {

    private static final String SHUTDOWN_MSG = "[Run ShutdownHook]";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            driver(true);  // test daemon will not prevent shutdown
            driver(false); // test non-daemon will prevent shutdown
        } else {
            assert Thread.currentThread().getName().equals("main");
            boolean daemon = Boolean.valueOf(args[0]);

            // start a non-daemon to cover the `--nonDaemonCount == 0` branch
            Thread thread = new Thread(() -> {/**/});
            thread.setDaemon(false);
            thread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println(SHUTDOWN_MSG)));
            thread = new Thread(() -> {
                System.out.println("thread started..");
                if (!daemon) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.setDaemon(daemon);
            thread.start();
            Thread.sleep(1000);
        }
    }

    private static void driver(boolean daemon) throws Exception {
        // we can not use jdk.testlibrary.ProcessTools here, because we need to analyse stdout of a unfinished process
        Process process = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseWisp2", "-cp", System.getProperty("java.class.path"), TestDaemon.class.getName(), Boolean.toString(daemon)).start();
        Thread.sleep(2000);
        byte[] buffer = new byte[1024];
        int n = process.getInputStream().read(buffer);
        String s = new String(buffer, 0, n);
        assertEQ(daemon, s.contains(SHUTDOWN_MSG));
    }
}
