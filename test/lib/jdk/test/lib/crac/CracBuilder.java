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
    public static final String ENV_LOG_FILE = "CRAC_LOG_FILE";

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
    boolean logToFile;
    String cracLogFile;
    //As opposite to one-stop mode is two steps mode that run application in docker
    // 1. docker run sleep 3600
    // 2. docker exec java xxxx
    // This mode has a flaw when checkpoint failed with message: "Error (criu/files-reg.c:1816): Can't lookup mount=24 for fd=0 path=/dev/null"
    // If run application with docker exec, the mnt_id of some fds are not in the /proc/$pid/mountinfo.
    // If run the one-stop mode, this problem is disappeared.
    boolean oneStopDockerRun;
    boolean allowSelfAttach;
    boolean restorePipeStdOutErr;

    boolean bumpPid;

    String appendOnlyFilesParam;

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

    public CracBuilder restorePipeStdOutErr(boolean restorePipeStdOutErr) {
        this.restorePipeStdOutErr = restorePipeStdOutErr;
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

    public CracBuilder logToFile(boolean logToFile) {
        this.logToFile = logToFile;
        assertNull(this.cracLogFile, "logToFile only can be set once!");
        this.cracLogFile = "crac-" + System.currentTimeMillis() + ".txt";
        return this;
    }

    public CracBuilder oneStopDockerRun(boolean oneStopDockerRun) {
        this.oneStopDockerRun = oneStopDockerRun;
        return this;
    }

    public CracBuilder bumpPid(boolean bumpPid) {
        this.bumpPid = bumpPid;
        return this;
    }

    public CracBuilder allowSelfAttach(boolean allow) {
        this.allowSelfAttach = allow;
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

    public CracBuilder appendOnlyFiles(String param) {
        assertNull(appendOnlyFilesParam);
        this.appendOnlyFilesParam = param;
        return this;
    }

    public void doCheckpoint() throws Exception {
        startCheckpoint().waitForCheckpointed();
    }

    public CracProcess startCheckpoint() throws Exception {
        return startCheckpoint(null);
    }

    public CracProcess startCheckpoint(List<String> javaPrefix) throws Exception {
        if (oneStopDockerRun) {
            return startOneStopCheckpoint(javaPrefix);
        }
        ensureContainerStarted();
        List<String> cmd = prepareCommand(javaPrefix);
        cmd.add("-XX:CRaCCheckpointTo=" + imageDir);
        cmd.add(main().getName());
        cmd.addAll(Arrays.asList(args()));
        log("Starting process to be checkpointed:");
        log(String.join(" ", cmd));
        return new CracProcess(this, cmd);
    }

    private CracProcess startOneStopCheckpoint(List<String> javaPrefix) throws Exception {
        assertNotNull(dockerImageName);
        assertNotNull(CRIU_PATH);
        ensureContainerKilled();
        DockerTestUtils.buildJdkContainerImage(dockerImageName, null,  "jdk-docker");
        FileUtils.deleteFileTreeWithRetry(Path.of(".", "jdk-docker"));
        // Make sure we start with a clean image directory
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "volume", "rm", "cr");

        List<String> runCmds = prepareCommonDockerRunCommands(javaPrefix);
        runCmds.add("-XX:CRaCCheckpointTo=" + imageDir);
        runCmds.add(main().getName());
        runCmds.addAll(Arrays.asList(args()));
        List<String> cmd = prepareContainerCommand(dockerImageName, dockerOptions,
                bumpPid ? wrapWithBumpPid(runCmds) : runCmds);
        log("Starting docker container to be checkpointed:\n" + String.join(" ", cmd));

        return new CracProcess(this, cmd);
    }

    private List<String> wrapWithBumpPid(List<String> runCmds) {
        runCmds.addAll(0, Arrays.asList("sh", "/test_src/bump_pid_run.sh"));
        return runCmds;
    }

    private List<String> prepareCommonDockerRunCommands(List<String> javaPrefix) {
        List<String> runCmds = new ArrayList<>();
        if (javaPrefix != null) {
            runCmds.addAll(javaPrefix);
        } else {
            runCmds.add(DOCKER_JAVA);
        }
        runCmds.add("-ea");
        runCmds.add("-cp");
        runCmds.add(getClassPath());
        if (engine != null) {
            runCmds.add("-XX:CREngine=" + engine.engine);
        }
        if (printResources) {
            runCmds.add("-XX:+UnlockDiagnosticVMOptions");
            runCmds.add("-XX:+CRPrintResourcesOnCheckpoint");
        }
        if (debug) {
            runCmds.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0.0.0.0:5005");
            runCmds.add("-XX:-CRDoThrowCheckpointException");
        }
        return runCmds;
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
            DockerTestUtils.buildJdkContainerImage(dockerImageName, null, "jdk-docker");
            FileUtils.deleteFileTreeWithRetry(Path.of(".", "jdk-docker"));
            // Make sure we start with a clean image directory
            DockerTestUtils.execute(Container.ENGINE_COMMAND, "volume", "rm", "cr");
            List<String> cmd = prepareDefaultContainerCommand(dockerImageName, dockerOptions);
            log("Starting docker container:\n" + String.join(" ", cmd));
            assertEquals(0, new ProcessBuilder().inheritIO().command(cmd).start().waitFor());
            containerStarted = true;
        }
    }

    private List<String> prepareDefaultContainerCommand(String imageName, String[] options) {
        return prepareContainerCommand(imageName, options, Arrays.asList("sleep", "3600"));
    }

    private List<String> prepareContainerCommand(String imageName, String[] options, List<String> runCommands) {
        List<String> cmd = new ArrayList<>();
        cmd.add(Container.ENGINE_COMMAND);
        cmd.addAll(Arrays.asList("run", "--rm", "-d"));
        cmd.add("--privileged"); // required to give CRIU sufficient permissions
        cmd.add("--init"); // otherwise the checkpointed process would not be reaped (by sleep with PID 1)
        int entryCounter = 0;
        for (var entry : Utils.TEST_CLASS_PATH.split(File.pathSeparator)) {
            cmd.addAll(Arrays.asList("--volume", entry + ":/cp/" + (entryCounter++)));
        }
        //bumpPid need bump_pid_run.sh,so need mount the test.src
        if (bumpPid) {
            cmd.addAll(Arrays.asList("--volume", Path.of(Utils.TEST_SRC).toString() + ":/test_src"));
        }
        cmd.addAll(Arrays.asList("--volume", "cr:/cr"));
        cmd.addAll(Arrays.asList("--volume", CRIU_PATH + ":/criu"));
        cmd.addAll(Arrays.asList("--env", "CRAC_CRIU_PATH=/criu"));
        cmd.addAll(Arrays.asList("--name", CONTAINER_NAME));
        if (logToFile) {
            cmd.addAll(Arrays.asList("--env", "CRAC_LOG_FILE=" + Path.of(System.getProperty("user.dir"), cracLogFile)));
            cmd.addAll(Arrays.asList("--volume", System.getProperty("user.dir") + ":" + System.getProperty("user.dir")));
        }
        if (debug) {
            cmd.addAll(Arrays.asList("--publish", "5005:5005"));
        }
        if (options != null) {
            cmd.addAll(Arrays.asList(options));
        }
        cmd.add(imageName);
        cmd.addAll(runCommands);
        return cmd;
    }

    public void ensureContainerKilled() throws Exception {
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "kill", CONTAINER_NAME).getExitValue();
        DockerTestUtils.removeDockerImage(dockerImageName);
    }

    public void deepClearContainer() throws Exception {
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "kill", CONTAINER_NAME).getExitValue();
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "rm", CONTAINER_NAME).getExitValue();
        DockerTestUtils.removeDockerImage(dockerImageName);
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "volume", "rm", "cr");
        //if don't execute prune command , there lots of files under overlay2.
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "system", "prune", "-f");
    }

    public void waitUntilContainerExit(long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeout) {
            String output = DockerTestUtils.execute(Container.ENGINE_COMMAND, "ps", "-f", "name=" + CONTAINER_NAME).getOutput();
            if (!output.contains(CONTAINER_NAME)) {
                return;
            }
            Thread.sleep(100);
        }
        fail("waitUntilContainerExit timeout for " + timeout + " ms.");
    }

    public void recreateContainer(String imageName, String... options) throws Exception {
        assertTrue(containerStarted);
        String minPid = DockerTestUtils.execute(Container.ENGINE_COMMAND, "exec", CONTAINER_NAME,
                "cat", "/proc/sys/kernel/ns_last_pid").getStdout().trim();
        DockerTestUtils.execute(Container.ENGINE_COMMAND, "kill", CONTAINER_NAME).getExitValue();
        List<String> cmd = prepareDefaultContainerCommand(imageName, options);
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
        if (oneStopDockerRun) {
            return startOneStopRestore(prefixJava);
        }
        ensureContainerStarted();
        List<String> cmd = prepareCommand(prefixJava);
        cmd.add("-XX:CRaCRestoreFrom=" + imageDir);
        log("Starting restored process:");
        log(String.join(" ", cmd));
        return new CracProcess(this, cmd);
    }

    private CracProcess startOneStopRestore(List<String> prefixJava) throws Exception {
        assertNotNull(dockerImageName);
        assertNotNull(CRIU_PATH);

        List<String> runCmds = prepareCommonDockerRunCommands(prefixJava);
        runCmds.add("-XX:CRaCRestoreFrom=" + imageDir);
        List<String> cmd = prepareContainerCommand(dockerImageName, dockerOptions, runCmds);
        log("Starting docker container to be restore:\n" + String.join(" ", cmd));
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
        if (allowSelfAttach) {
            cmd.add("-Djdk.attach.allowAttachSelf=true");
        }
        if (restorePipeStdOutErr) {
            cmd.add("-XX:CRaCRestoreInheritPipeFds=1,2");
        }
        if (appendOnlyFilesParam != null) {
            cmd.add("-XX:CRaCAppendOnlyLogFiles=" + appendOnlyFilesParam);
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
