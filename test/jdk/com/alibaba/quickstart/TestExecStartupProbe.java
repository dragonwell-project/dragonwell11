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
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @test
 * @summary test startupProbe of quickstart
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestExecStartupProbe
 */
public class TestExecStartupProbe implements SingleProjectProvider {
    private final String succKeyWord;
    private final String logFile;

    public TestExecStartupProbe(String succKeyWord, String logFile) {
        this.succKeyWord = succKeyWord;
        this.logFile = logFile;
    }

    public static void main(String[] args) throws Exception {
        String logFile = System.getProperty("user.dir") + File.separator + "app.log";
        final String succKeyWord = "startup successful";
        String probeJSON = String.format("{" +
                "\"exec\": {" +
                "    \"command\": [\"grep\",\"%s\",\"%s\"]" +
                "}," +
                "  \"initialDelaySeconds\": 3," +
                "  \"timeoutSeconds\": 3," +
                "  \"periodSeconds\": 3" +
                "}", succKeyWord, logFile);

        String probeBase64 = Base64.getEncoder().encodeToString(probeJSON.getBytes(StandardCharsets.UTF_8));
        new StartupProbeTestRunner(QuickStartFeature.FULL, probeBase64).run(new TestExecStartupProbe(succKeyWord, logFile));
    }

    @Override
    public Project getProject() {
        final String mainClass = "com.App1";
        String sourceTemplate =
                "package com;\n" +
                        "import java.io.FileWriter;\n" +
                        "public class App1{ \n" +
                        "    public static void main(String[] args) throws Exception {\n" +
                        "        FileWriter fw = new FileWriter(\"%s\");\n" +
                        "        fw.write(\"%s\");\n" +
                        "        fw.close();\n" +
                        "        Thread.sleep(10 * 1000);\n" +
                        "    }" +
                        " }";


        Project project = new Project(new RunWithURLClassLoaderConf(mainClass),
                new Artifact[]{Artifact.createPlainJar("app", "app-lib", "app.1.0.jar", null,
                        new JavaSource[]{
                                new JavaSource(mainClass, String.format(sourceTemplate, logFile, succKeyWord))
                        },
                        new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})},
                new
                        ExpectOutput(new String[]{}));

        return project;
    }


}
