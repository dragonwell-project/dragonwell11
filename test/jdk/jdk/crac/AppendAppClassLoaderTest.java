import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracProcess;
import jdk.test.lib.crac.CracTest;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

import java.lang.reflect.Method;
import java.nio.file.Paths;

import jdk.crac.*;

/**
 * @test
 * @summary Append to app classloader when restore.
 * @library /test/lib
 * @compile ./Foo.java
 * @build AppendAppClassLoaderTest
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest
 */
public class AppendAppClassLoaderTest implements CracTest {
    private final static String TEST_DIR = System.getProperty("test.classes");
    private final String FOO_JAR = "foo.jar";
    private Resource resource;

    @Override
    public void test() throws Exception {
        JarUtils.createJarFile(Paths.get(FOO_JAR), Paths.get(TEST_DIR), Paths.get(TEST_DIR, "Foo.class"));
        //remove it avoid Foo in app classpath before call Core.appendToAppClassLoaderClassPath
        Paths.get(TEST_DIR, "Foo.class").toFile().delete();
        CracBuilder cracBuilder = new CracBuilder().captureOutput(true)
                                          .restorePipeStdOutErr(true);
        CracProcess crProcess = cracBuilder.startCheckpoint();
        crProcess.waitForCheckpointed();
        OutputAnalyzer crOutputAnalyzer = crProcess.outputAnalyzer();
        crOutputAnalyzer.shouldContain("Foo should not found");

        CracProcess restoreProcess = cracBuilder.doRestore();
        OutputAnalyzer restoreOutputAnalyzer = restoreProcess.outputAnalyzer();
        restoreOutputAnalyzer.shouldContain("A msg from Foo#hello");
    }

    @Override
    public void exec() throws Exception {
        try {
            Class fooClass = ClassLoader.getSystemClassLoader().loadClass("Foo");
            //should not reach here
            System.out.println(fooClass);
        } catch (ClassNotFoundException cnf) {
            System.out.println("Foo should not found");
        }
        resource = new Resource() {
            @Override
            public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            }

            @Override
            public void afterRestore(Context<? extends Resource> context) throws Exception {
                Core.appendToAppClassLoaderClassPath(FOO_JAR);
            }
        };
        Core.getGlobalContext().register(resource);
        Core.checkpointRestore();
        Class fooClass = ClassLoader.getSystemClassLoader().loadClass("Foo");
        Object o = fooClass.getConstructor().newInstance();
        Method m = fooClass.getDeclaredMethod("hello");
        m.invoke(o);
    }
}
