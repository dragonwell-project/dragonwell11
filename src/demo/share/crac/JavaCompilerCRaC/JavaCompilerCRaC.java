import java.util.Arrays;
import jdk.crac.Core;

public class JavaCompilerCRaC {

    static void runJavac(String... args) {
        System.out.println("javac " + String.join(" ", args));
        int status = com.sun.tools.javac.Main.compile(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    public static void main(String... args) throws Exception {
        int startIdx = 0;
        for (int endIdx = 1; endIdx < args.length; ++endIdx) {
            if (args[endIdx].equals("--")) {
                runJavac(Arrays.copyOfRange(args, startIdx, endIdx));
                startIdx = endIdx + 1;
            }
        }

        if (startIdx < args.length) {
            runJavac(Arrays.copyOfRange(args, startIdx, args.length));
        }

        Core.checkpointRestore();
    }
}
