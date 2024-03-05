import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracProcess;
import jdk.test.lib.crac.CracTest;

import javax.crac.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * @test
 * @library /test/lib
 * @build PseudoPersistentFileTest
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest
 */
public class PseudoPersistentFileTest implements CracTest {

    private Resource resource;

    @Override
    public void test() throws Exception {
        CracBuilder cracBuilder = new CracBuilder().captureOutput(true)
                                          .restorePipeStdOutErr(true);
        CracProcess crProcess = cracBuilder.startCheckpoint();
        crProcess.waitForCheckpointed();
        cracBuilder.doRestore();
    }

    @Override
    public void exec() throws Exception {
        File f1 = new File("f1.txt");
        BufferedWriter bw1 = new BufferedWriter(new FileWriter(f1));
        bw1.write("file f1");

        File f2 = new File("f2.txt");
        BufferedWriter bw2 = new BufferedWriter(new FileWriter(f2));
        bw2.write("file f2");

        resource = new Resource() {
            @Override
            public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
                Core.registerPseudoPersistent(f1.getAbsolutePath(), Core.SAVE_RESTORE | Core.COPY_WHEN_RESTORE);
                Core.registerPseudoPersistent(f2.getAbsolutePath(), Core.SAVE_RESTORE | Core.SYMLINK_WHEN_RESTORE);
            }

            @Override
            public void afterRestore(Context<? extends Resource> context) throws Exception {
            }
        };
        Core.getGlobalContext().register(resource);
        Core.checkpointRestore();
        bw1.close();
        bw2.close();
    }
}
