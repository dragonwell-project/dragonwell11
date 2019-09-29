/*
 * @test
 * @library /lib/testlibrary
 * @summary Test wisp worker thread isn't created without -Dcom.alibaba.wisp.transparentWispSwitch=true
 * @run main/othervm -XX:ActiveProcessorCount=2 TestWorkerContainer
 */

import com.alibaba.wisp.engine.WispWorkerContainer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static jdk.testlibrary.Asserts.assertFalse;

public class TestWorkerContainer {

    private static void test() throws Exception {
        Class.forName(WispWorkerContainer.class.getName());

        Thread.sleep(100);

        List<String> result = jstack();

        int i = 0;
        for (; i < result.size(); i++) {
            System.out.println(result.get(i));
            assertFalse(result.get(i).contains("WISP_WORKER_"));
        }
    }

    private static List<String> jstack() throws Exception {
        List<String> statusLines = Files.readAllLines(Paths.get("/proc/self/status"));
        String pidLine = statusLines.stream().filter(l -> l.startsWith("Pid:")).findAny().orElse("1 -1");
        int pid = Integer.valueOf(pidLine.split("\\s+")[1]);

        Process p = Runtime.getRuntime().exec(System.getProperty("java.home") + "/bin/jstack " + pid);
        List<String> result = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().collect(Collectors.toList());
        return result;
    }

    public static void main(String[] args) throws Exception {
        test();
    }
}
