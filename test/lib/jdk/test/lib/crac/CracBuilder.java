package jdk.test.lib.crac;

import jdk.test.lib.Container;
import jdk.test.lib.Utils;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static jdk.test.lib.Asserts.*;

public class CracBuilder {
    private static final String DEFAULT_IMAGE_DIR = "cr";
    public static final String CONTAINER_NAME = "crac-test";
    public static final String JAVA = Utils.TEST_JDK + "/bin/java";
    public static final String DOCKER_JAVA = "/jdk/bin/java";
    private static final List<String> CRIU_CANDIDATES = Arrays.asList(Utils.TEST_JDK + "/lib/criu", "/usr/sbin/criu", "/sbin/criu");
    private static final String CRIU_PATH;

    // This dummy field is here as workaround for (possibly) a JTReg bug;
    // some tests don't build CracTestArg into their Test.d/ directory
    // (not all classes from /test/lib are built!) and the tests would fail.
    // This does not always happen when the test is run individually but breaks
    // when the whole suite is executed.
    private static final Class<CracTestArg> dummyWorkaround = CracTestArg.class;

    boolean verbose = true;
    boolean debug = false;
    final List<String> classpathEntries = new ArrayList<>();
    final Map<String, String> env = new HashMap<>();
    String imageDir = DEFAULT_IMAGE_DIR;
    CracEngine engine;
    boolean printResources;
    Class<?> main;
    String[] args;
    boolean captureOutput;
    String dockerImageName;
    private String[] dockerOptions;

    boolean containerStarted;

    static {
        String path = System.getenv("CRAC_CRIU_PATH");
        if (path == null) {
            for (String candidate : CRIU_CANDIDATES) {
                if (new File(candidate).exists()) {
                    path = candidate;
                    break;
                }
            }
        }
        CRIU_PATH = path;
    }

    public CracBuilder() {
    }

    public CracBuilder verbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public CracBuilder debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public CracBuilder classpathEntry(String cp) {
        classpathEntries.add(cp);
        return this;
    }

    public CracBuilder engine(CracEngine engine) {
        assertNull(this.engine); // set once
        this.engine = engine;
        return this;
    }

    public Path imageDir() {
        return Path.of(imageDir);
    }

    public CracBuilder imageDir(String imageDir) {
        assertEquals(DEFAULT_IMAGE_DIR, this.imageDir); // set once
        this.imageDir = imageDir;
        return this;
    }

    public CracBuilder printResources(boolean print) {
        this.printResources = print;
        return this;
    }

    public CracBuilder env(String name, String value) {
        env.put(name, value);
        return this;
    }

    public CracBuilder main(Class<?> mainClass) {
        assertNull(this.main); // set once
        this.main = mainClass;
        return this;
    }

    public Class<?> main() {
        return main != null ? main : CracTest.class;
    }

    public CracBuilder args(String... args) {
        assertNull(this.args); // set once
        this.args = args;
        return this;
    }

    public String[] args() {
        return args != null ? args : CracTest.args();
    }

    public CracBuilder captureOutput(boolean captureOutput) {
        this.captureOutput = captureOutput;
        return this;
    }

    public CracBuilder inDockerImage(String imageName) {
        assertNull(dockerImageName);
        this.dockerImageName = imageName;
        return this;
    }

    public CracBuilder dockerOptions(String... options) {
        assertNull(dockerOptions);
        this.dockerOptions = options;
        return this;
    }

    public void doCheckpoint() throws Exception {
        startCheckpoint().waitForCheckpointed();
    }

    public CracProcess startCheckpoint() throws Exception {
        return startCheckpoint(null);
    }

    public CracProcess startCheckpoint(List<String> javaPrefix) throws Exception {
        ensureContainerStarted();
        List<String> cmd = prepareCommand(javaPrefix);
        cmd.add("-XX:CRaCCheckpointTo=" + imageDir);
        cmd.add(main().getName());
        cmd.addAll(Arrays.asList(args()));
        log("Starting process to be checkpointed:");
        log(String.join(" ", cmd));
        return new CracProcess(this, cmd);
    }

    void log(String fmt, Object... args) {
        if (verbose) {
            if (args.length == 0) {
                System.err.println(fmt);
            } else {
                System.err.printf(fmt, args);
            }
        }
    }

    private void ensureContainerStarted() throws Exception {
        if (dockerImageName == null) {
            return;
        }
        if (CRIU_PATH == null) {
            fail("CRAC_CRIU_PATH is not set and cannot find criu executable in any of: " + CRIU_CANDIDATES);
        }
        if (!containerStarted) {
            ensureContainerKilled();
            DockerTestUtils.buildJdkContainerImage(dockerImageName, null);
            FileUtils.deleteFileTreeWithRetry(Path.of(".", "jdk-docker"));
            // Make sure we start with a clean image directory
            DockerTestUtils.execute(Container.ENGINE_COMMAND, "volume", "rm", "cr");
            List<String> cmd = prepareContainerCommand(dockerImageName, dockerOptions);
            log("Starting docker container:\n" + String.join(" ", cmd));
            assertEquals(0, new ProcessBuilder().inheritIO().command(cmd).start().waitFor());
            containerStarted = true;
        }
    }

    private List<String> prepareContainerCommand(String imageName, String[] options) {
        List<String> cmd = new ArrayList<>();
        cmd.add(Container.ENGINE_COMMAND);
        cmd.addAll(Arrays.asList("run", "--rm", "-d"));
        cmd.add("--privileged"); // required to give CRIU sufficient permissions
        cmd.add("--init"); // otherwise the checkpointed process would not be reaped (by sleep with PID 1)
        int entryCounter = 0;
        for (var entry : Utils.TEST_CLASS_PATH.split(File.pathSeparator)) {
            cmd.addAll(Arrays.asList("--volume", entry + ":/cp/" + (entryCounter++)));
        }
        cmd.addAll(Arrays.asList("--volume", "cr:/cr"));
        cmd.addAll(Arrays.asList("--volume", CRIU_PATH + ":/criu"));
        cmd.addAll(Arrays.asList("--env", "CRAC_CRIU_PATH=/criu"));
        cmd.addAll(Arrays.asList("--name", CONTAINER_NAME));
        if (debug) {
            cmd.addAll(Arrays.asList("--publish", "5005:5005"));
        }
        if (options != null) {
            cmd.addAll(Arrays.asList(options));
        }
        cmd.add(imageName);
        cmd.addAll(Arrays.asList("sleep", "3600"));
        return cmd;
    }

    public void ensureContainerKilled() throws Exception {
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "kill", CONTAINER_NAME).getExitValue();
        DockerTestUtils.removeDockerImage(dockerImageName);
    }

    public void recreateContainer(String imageName, String... options) throws Exception {
        assertTrue(containerStarted);
        String minPid = DockerTestUtils.execute(Container.ENGINE_COMMAND, "exec", CONTAINER_NAME,
                "cat", "/proc/sys/kernel/ns_last_pid").getStdout().trim();
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "kill", CONTAINER_NAME).getExitValue();
        List<String> cmd = prepareContainerCommand(imageName, options);
        log("Recreating docker container:\n" + String.join(" ", cmd));
        assertEquals(0, new ProcessBuilder().inheritIO().command(cmd).start().waitFor());
        // We need to cycle PIDs; had we tried to restore right away the exec would get the
        // same PIDs and restore would fail.
        log("Cycling PIDs until %s%n", minPid);
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "exec",
                CONTAINER_NAME, "bash", "-c",
                "while [ $(cat /proc/sys/kernel/ns_last_pid) -le " + minPid + " ]; do cat /dev/null; done");
    }

    public CracProcess doRestore() throws Exception {
        return startRestore().waitForSuccess();
    }

    public CracProcess startRestore() throws Exception {
         return startRestore(null);
    }
    public CracProcess startRestore(List<String> prefixJava) throws Exception {
        ensureContainerStarted();
        List<String> cmd = prepareCommand(prefixJava);
        cmd.add("-XX:CRaCRestoreFrom=" + imageDir);
        log("Starting restored process:");
        log(String.join(" ", cmd));
        return new CracProcess(this, cmd);
    }

    public CracProcess startPlain() throws IOException {
        List<String> cmd = new ArrayList<>();
        if (dockerImageName != null) {
            cmd.addAll(Arrays.asList(Container.ENGINE_COMMAND, "exec", CONTAINER_NAME));
        }
        cmd.add(JAVA);
        cmd.add("-ea");
        cmd.add("-cp");
        cmd.add(getClassPath());
        if (debug) {
            cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0.0.0.0:5005");
        }
        cmd.add(main().getName());
        cmd.addAll(Arrays.asList(args()));
        log("Starting process without CRaC:");
        log(String.join(" ", cmd));
        return new CracProcess(this, cmd);
    }

    private String getClassPath() {
        String classPath = classpathEntries.isEmpty() ? "" : String.join(File.pathSeparator, classpathEntries) + File.pathSeparator;
        if (dockerImageName == null) {
            classPath += Utils.TEST_CLASS_PATH;
        } else {
            int numEntries = Utils.TEST_CLASS_PATH.split(File.pathSeparator).length;
            for (int i = 0; i < numEntries; ++i) {
                classPath += "/cp/" + i + File.pathSeparator;
            }
        }
        return classPath;
    }

    public CracProcess doPlain() throws IOException, InterruptedException {
        return startPlain().waitForSuccess();
    }

    private List<String> prepareCommand(List<String> javaPrefix) {
        List<String> cmd = new ArrayList<>();
        if (javaPrefix != null) {
            cmd.addAll(javaPrefix);
        } else if (dockerImageName != null) {
            cmd.addAll(Arrays.asList(Container.ENGINE_COMMAND, "exec", CONTAINER_NAME));
            cmd.add(DOCKER_JAVA);
        } else {
            cmd.add(JAVA);
        }
        cmd.add("-ea");
        cmd.add("-cp");
        cmd.add(getClassPath());
        if (engine != null) {
            cmd.add("-XX:CREngine=" + engine.engine);
        }
        if (printResources) {
            cmd.add("-XX:+UnlockDiagnosticVMOptions");
            cmd.add("-XX:+CRPrintResourcesOnCheckpoint");
        }
        if (debug) {
            cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0.0.0.0:5005");
            cmd.add("-XX:-CRDoThrowCheckpointException");
        }
        return cmd;
    }

    public void doCheckpointAndRestore() throws Exception {
        doCheckpoint();
        doRestore();
    }

    public void checkpointViaJcmd() throws Exception {
        List<String> cmd = new ArrayList<>();
        if (dockerImageName != null) {
            cmd.addAll(Arrays.asList(Container.ENGINE_COMMAND, "exec", CONTAINER_NAME, "/jdk/bin/jcmd"));
        } else {
            cmd.add(Utils.TEST_JDK + "/bin/jcmd");
        }
        cmd.addAll(Arrays.asList(main().getName(), "JDK.checkpoint"));
        // This works for non-docker commands, too
        DockerTestUtils.execute(cmd).shouldHaveExitValue(0);
    }

}
