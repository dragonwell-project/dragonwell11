/*
 * @test
 * @summary Test AppCDS/EagerAppCDS with multi-versioned jars, when JVMTI agents that transform classes are enabled.
 * @library /lib/testlibrary /test/lib /lib/testlibrary/java/util/jar
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @modules java.base/sun.security.action
 * @modules jdk.compiler
 * @modules java.base/com.alibaba.util:+open
 * @modules java.base/jdk.internal.loader:+open
 * @build MyWebAppClassLoader
 * @build LoadMultiVersionedJarClass
 * @build Compiler JarBuilder CreateMultiReleaseTestJars
 * @requires os.arch=="amd64" | os.arch=="aarch64"
 * @run driver ClassFileInstaller -jar test.jar LoadMultiVersionedJarClass MyWebAppClassLoader
 * @run main/othervm -XX:+UnlockExperimentalVMOptions TestMultiVersionedJar
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.security.action.GetPropertyAction;

import java.security.AccessController;

public class TestMultiVersionedJar {

    private static final String TESTJAR = "test.jar";
    private static final String TESTNAME = "LoadMultiVersionedJarClass";

    public static void main(String[] args) throws Exception {

        String dir = AccessController.doPrivileged(new GetPropertyAction("test.classes"));
        destroyCache(dir);

        CreateMultiReleaseTestJars creator = new CreateMultiReleaseTestJars();
        creator.compileEntries();
        creator.buildMultiReleaseJar();

        trace(dir);

        replay(dir);
    }

    static void trace(String parentDir) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(true,
            "-Xquickstart:path=" + parentDir + "/quickstartcache",
            "-Xquickstart:verbose",
            "-Xlog:cds+jvmti=debug",
            "-Xlog:eagerappcds+jvmti=debug",
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "-cp",
            TESTJAR,
            TESTNAME);

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println(output.getOutput());
        output.shouldHaveExitValue(0);
    }

    static void replay(String parentDir) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(true,
            "-Xquickstart:path=" + parentDir + "/quickstartcache",
            "-Xquickstart:verbose",
            "-Xlog:class+eagerappcds=trace",
            "-Xlog:cds+jvmti=debug",
            "-Xlog:eagerappcds+jvmti=debug",
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+PrintEagerAppCDSExceptions",
            "-cp",
            TESTJAR,
            TESTNAME);

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println(output.getOutput());
        output.shouldHaveExitValue(0);
    }

    static void destroyCache(String parentDir) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Xquickstart:destroy",
                "-Xquickstart:path=" + parentDir + "/quickstartcache",
                "-Xquickstart:verbose", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("destroy the cache folder");
        output.shouldHaveExitValue(0);
    }

}
