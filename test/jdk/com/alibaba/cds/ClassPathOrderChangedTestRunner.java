import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class ClassPathOrderChangedTestRunner extends QuickStartTestRunner {

    private final Function<List<String>, List<String>> traceCPTransformer;
    private final Function<List<String>, List<String>> replayCPTransformer;
    ProjectWorkDir projectWorkDir;

    ClassPathOrderChangedTestRunner(Function<List<String>,List<String>> traceCPTransformer, Function<List<String>,List<String>> replayCPTransformer) {
        this.traceCPTransformer = traceCPTransformer;
        this.replayCPTransformer = replayCPTransformer;
    }


    @Override
    protected void run(SingleProjectProvider projectProvider) throws Exception {
        String workDir = System.getProperty("user.dir");
        projectWorkDir = new ProjectWorkDir(workDir + File.separator + "a");
        Project project = projectProvider.getProject();
        project.build(projectWorkDir);
        runAsTracer(project, projectWorkDir, true);
        runAsReplayer(project, projectWorkDir, true);
    }

    protected void runAsTracer(Project p, ProjectWorkDir workDir, boolean doClassDiff) throws Exception {
        String[] commands = p.getRunConf().buildJavaRunCommands(workDir.getBuild(), p.getArtifacts());
        List<String> cp = p.getRunConf().classpath(workDir.getBuild(), p.getArtifacts());
        cp = traceCPTransformer.apply(cp);
        ProcessBuilder pb = createJavaProcessBuilder(cp, merge(new String[][]{
                getQuickStartOptions(workDir.getCacheDir(), doClassDiff), commands}));
        jdk.test.lib.process.OutputAnalyzer output = new jdk.test.lib.process.OutputAnalyzer(pb.start());
        output.shouldContain("Running as tracer");
        output.shouldHaveExitValue(0);
    }

    protected void runAsReplayer(Project p, ProjectWorkDir workDir, boolean doClassDiff) throws IOException {
        String[] commands = p.getRunConf().buildJavaRunCommands(workDir.getBuild(), p.getArtifacts());
        List<String> cp = p.getRunConf().classpath(workDir.getBuild(), p.getArtifacts());
        cp = replayCPTransformer.apply(cp);
        ProcessBuilder pb = createJavaProcessBuilder(cp, merge(new String[][]{
                getQuickStartOptions(workDir.getCacheDir(), doClassDiff), commands}));
        jdk.test.lib.process.OutputAnalyzer output = new jdk.test.lib.process.OutputAnalyzer(pb.start());
        System.out.println("==========   QuickStart Output   ==========");
        System.out.println(output.getOutput());
        System.out.println("========== QuickStart Output End ==========");
        output.shouldContain("Running as replayer");
        output.shouldHaveExitValue(0);
        if (p.getExpectOutput() != null) {
            for (String expect : p.getExpectOutput().getExpectLines()) {
                output.shouldContain(expect);
            }
            if (p.getExpectOutput().getShouldNotContainLines() != null) {
                for (String notExpect : p.getExpectOutput().getShouldNotContainLines()) {
                    output.shouldNotContain(notExpect);
                }
            }
        }
    }

    @Override
    protected void run(PairProjectProvider projectProvider) throws Exception {
        throw new RuntimeException("Not support!");
    }

    @Override
    public String[] getQuickStartOptions(File cacheDir, boolean dummy) {
        return new String[]{"-Xquickstart:path=" + cacheDir.getAbsolutePath(), "-XX:-AppCDSVerifyClassPathOrder", "-XX:+IgnoreAppCDSDirCheck", "-Xquickstart:verbose", "-Xlog:class+eagerappcds=trace"};
    }
}
