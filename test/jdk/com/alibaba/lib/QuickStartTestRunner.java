/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class QuickStartTestRunner {
    private static volatile Object garbage;
    protected String workDir;

    public void runTest(SingleProjectProvider projectProvider) throws Exception {
        this.workDir = System.getProperty("user.dir");
        forceCompileClass();
        run(projectProvider);
        postCheck();
    }

    public void runTest(PairProjectProvider projectProvider) throws Exception {
        this.workDir = System.getProperty("user.dir");
        forceCompileClass();
        run(projectProvider);
        postCheck();
    }


    abstract protected void run(SingleProjectProvider projectProvider) throws Exception;

    abstract protected void run(PairProjectProvider projectProvider) throws Exception;

    void postCheck() throws Exception {
    }


    /**
     * some classes that used by dynamic compiled java source that construct from common.JavaSource.
     * If there no depend on these classes,it will not compile.
     * Also we can add @build to each test class,but it's tedious.
     */
    private void forceCompileClass() {
        garbage = JarLauncher.class;
        garbage = FatJarHandler.class;
        garbage = URLClassLoaderLauncher.class;
        garbage = EmptyURLsURLClassLoadLauncher.class;
    }


    abstract String[] getQuickStartOptions(File cacheDir, boolean doClassDiff);

    protected String[] merge(String[][] arrays) {
        int total = 0;
        for (int i = 0; i < arrays.length; i++) {
            total += arrays[i].length;
        }
        String[] fullCommands = new String[total];
        int start = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, fullCommands, start, arrays[i].length);
            start += arrays[i].length;
        }
        return fullCommands;
    }

    protected void runAsTracer(Project p, ProjectWorkDir workDir, boolean doClassDiff) throws Exception {
        String[] commands = p.getRunConf().buildJavaRunCommands(workDir.getBuild(), p.getArtifacts());
        List<String> cp = p.getRunConf().classpath(workDir.getBuild(), p.getArtifacts());
        ProcessBuilder pb = createJavaProcessBuilder(cp, merge(new String[][]{
                getQuickStartOptions(workDir.getCacheDir(), doClassDiff), commands}));
        jdk.test.lib.process.OutputAnalyzer output = new jdk.test.lib.process.OutputAnalyzer(pb.start());
        output.shouldContain("Running as tracer");
        output.shouldHaveExitValue(0);
    }

    protected void runAsReplayer(Project p, ProjectWorkDir workDir, boolean doClassDiff) throws IOException {
        String[] commands = p.getRunConf().buildJavaRunCommands(workDir.getBuild(), p.getArtifacts());
        List<String> cp = p.getRunConf().classpath(workDir.getBuild(), p.getArtifacts());
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

    protected void destroyCache(String cachePath) throws Exception {
        ProcessBuilder pb = createJavaProcessBuilder(null,
                merge(new String[][]{
                        new String[]{"-Xquickstart:destroy", "-Xquickstart:path=" + cachePath},
                        new String[]{"-version"}
                }));
        jdk.test.lib.process.OutputAnalyzer output = new jdk.test.lib.process.OutputAnalyzer(pb.start());
        output.shouldContain("destroy the cache folder");
        output.shouldHaveExitValue(0);
    }

    protected ProcessBuilder createJavaProcessBuilder(List<String> moreCP, String... command) {
        String javapath = jdk.test.lib.JDKToolFinder.getJDKTool("java");

        ArrayList<String> args = new ArrayList<>();
        args.add(javapath);

        args.add("-cp");
        if (moreCP != null) {
            args.add(System.getProperty("java.class.path") + File.pathSeparator + String.join(File.pathSeparator, moreCP));
        } else {
            args.add(System.getProperty("java.class.path"));
        }

        Collections.addAll(args, command);

        // Reporting
        StringBuilder cmdLine = new StringBuilder();
        for (String cmd : args)
            cmdLine.append(cmd).append(' ');
        System.out.println("Command line: [" + cmdLine.toString() + "]");
        return new ProcessBuilder(args.toArray(new String[args.size()]));
    }

    protected ProcessBuilder createJavaProcessBuilderNoCP(String... command) {
        String javapath = jdk.test.lib.JDKToolFinder.getJDKTool("java");

        ArrayList<String> args = new ArrayList<>();
        args.add(javapath);
        Collections.addAll(args, command);

        // Reporting
        StringBuilder cmdLine = new StringBuilder();
        for (String cmd : args)
            cmdLine.append(cmd).append(' ');
        System.out.println("Command line: [" + cmdLine.toString() + "]");
        return new ProcessBuilder(args.toArray(new String[args.size()]));
    }
}
