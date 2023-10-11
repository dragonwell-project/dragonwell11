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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class StartupProbeTestRunner extends QuickStartTestRunner {
    ProjectWorkDir projectWorkDir;
    private QuickStartFeature feature;
    String base64StartupProbe;

    public StartupProbeTestRunner(QuickStartFeature feature,String base64StartupProbe) {
        this.feature = feature;
        this.base64StartupProbe = base64StartupProbe;
    }

    @Override
    protected void run(SingleProjectProvider projectProvider) throws Exception {
        String workDir = System.getProperty("user.dir");
        projectWorkDir = new ProjectWorkDir(workDir + File.separator + "a");
        Project project = projectProvider.getProject();
        project.build(projectWorkDir);
        runAsProfile(project, projectWorkDir);
        runAsReplayer(project, projectWorkDir, false);
    }

    @Override
    protected void run(PairProjectProvider projectProvider) throws Exception {
        throw new RuntimeException("Not support!");
    }

    @Override
    public String[] getQuickStartOptions(File cacheDir, boolean doClassDiff) {
        return merge(new String[][]{feature.getAppendixOption(),
                new String[]{"-Xquickstart:verbose,path=" + cacheDir.getAbsolutePath() + append(feature.getQuickstartSubOption())}});
    }

    private String append(String subOption) {
        return null == subOption || "".equals(subOption) ? "" : "," + subOption;
    }

    private String[] getProfileOptions(File cacheDir) {
        return merge(new String[][]{feature.getAppendixOption(),
                new String[]{"-Xquickstart:profile,verbose,path=" + cacheDir.getAbsolutePath() + append(feature.getQuickstartSubOption())}});
    }

    @Override
    void postCheck() throws Exception {
        //check if all classes and jars has been removed.
        List<Path> files = Files.walk(projectWorkDir.getCacheDir().toPath())
                .filter((f) -> (f.endsWith(".jar") || f.endsWith(".class")))
                .collect(Collectors.toList());
        if (!files.isEmpty()) {
            files.forEach((f) -> System.out.println("Found file :" + f.toFile().getAbsolutePath() + " in cache directory!"));
            throw new RuntimeException("Cache directory " + projectWorkDir.getCacheDir().getAbsolutePath() + " should not contain jars and class file!");
        }
    }

    protected void runAsProfile(Project p, ProjectWorkDir workDir) throws Exception {
        String[] commands = p.getRunConf().buildJavaRunCommands(workDir.getBuild(), p.getArtifacts());
        List<String> cp = p.getRunConf().classpath(workDir.getBuild(), p.getArtifacts());
        ProcessBuilder pb = createJavaProcessBuilder(cp, merge(new String[][]{
                getProfileOptions(workDir.getCacheDir()), commands}));
        pb.environment().put("DRAGONWELL_QUICKSTART_STARTUP_PROBE",base64StartupProbe);
        //set a file flag that indicate CDS dump successful
        pb.environment().put("CDS_DUMP_FINISH_FILE", workDir.getCacheDir() + File.separator + "metadata");
        jdk.test.lib.process.OutputAnalyzer output = new jdk.test.lib.process.OutputAnalyzer(pb.start());
        output.shouldContain("Running as profiler");
        output.shouldHaveExitValue(0);
    }
}