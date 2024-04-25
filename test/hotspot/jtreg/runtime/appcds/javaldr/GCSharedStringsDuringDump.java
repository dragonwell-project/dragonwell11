/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Similar to GCDuringDumping.java, this test adds the -XX:SharedArchiveConfigFile
 *          option for testing the interaction with GC and shared strings.
 * @library /test/lib /test/hotspot/jtreg/runtime/appcds /test/hotspot/jtreg/runtime/appcds/test-classes
 * @requires vm.cds.archived.java.heap
 * @modules java.base/jdk.internal.misc
 *          jdk.jartool/sun.tools.jar
 *          java.management
 * @build sun.hotspot.WhiteBox GCDuringDumpTransformer GCSharedStringsDuringDumpWb
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=480 GCSharedStringsDuringDump
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

public class GCSharedStringsDuringDump {
    public static String appClasses[] = {
        "GCSharedStringsDuringDumpWb",
    };
    public static String agentClasses[] = {
        "GCDuringDumpTransformer",
    };

    public static void main(String[] args) throws Throwable {
        String agentJar =
            ClassFileInstaller.writeJar("GCDuringDumpTransformer.jar",
                                        ClassFileInstaller.Manifest.fromSourceFile("GCDuringDumpTransformer.mf"),
                                        agentClasses);

        String appJar =
            ClassFileInstaller.writeJar("GCSharedStringsDuringDumpApp.jar", appClasses);

        String gcLog = Boolean.getBoolean("test.cds.verbose.gc") ?
            "-Xlog:gc*=info,gc+region=trace,gc+alloc+region=debug" : "-showversion";

        String sharedArchiveCfgFile =
            System.getProperty("user.dir") + File.separator + "GCSharedStringDuringDump_gen.txt";
        try (FileOutputStream fos = new FileOutputStream(sharedArchiveCfgFile)) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(fos));
            out.println("VERSION: 1.0");
            out.println("@SECTION: String");
            out.println("31: shared_test_string_unique_14325");
            for (int i=0; i<100000; i++) {
                String s = "generated_string " + i;
                out.println(s.length() + ": " + s);
            }
            out.close();
        }

        JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");
        String whiteBoxJar = TestCommon.getTestJar("WhiteBox.jar");
        String bootClassPath = "-Xbootclasspath/a:" + whiteBoxJar;

        for (int i=0; i<2; i++) {
            // i = 0 -- run without agent = no extra GCs
            // i = 1 -- run with agent = cause extra GCs

            String extraArg = (i == 0) ? "-showversion" : "-javaagent:" + agentJar;

            OutputAnalyzer output = TestCommon.dump(
                                appJar, TestCommon.list("GCSharedStringsDuringDumpWb"),
                                bootClassPath, extraArg, "-Xmx32m", gcLog,
                                "-XX:SharedArchiveConfigFile=" + sharedArchiveCfgFile);

            if (output.getStdout().contains("Too many string space regions") ||
                output.getStderr().contains("Unable to write archive heap memory regions") ||
                output.getStdout().contains("Try increasing NewSize") ||
                !output.getStdout().contains("oa0 space:") ||
                output.getExitValue() != 0) {
                // Try again with larger heap and NewSize, this should increase the
                // G1 heap region size to 2M
                TestCommon.testDump(
                    appJar, TestCommon.list("GCSharedStringsDuringDumpWb"),
                    bootClassPath, extraArg, "-Xmx8g", "-XX:NewSize=8m", gcLog,
                    "-XX:SharedArchiveConfigFile=" + sharedArchiveCfgFile);
            }

            TestCommon.run(
                "-cp", appJar,
                bootClassPath,
                "-Xmx32m",
                "-XX:+PrintSharedSpaces",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-XX:SharedReadOnlySize=30m",
                gcLog,
                "GCSharedStringsDuringDumpWb")
              .assertNormalExit();
        }
    }
}

