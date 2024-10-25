/*
 * Copyright (c) 2024 Alibaba Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Alibaba designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package sanity;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
 * @test
 * @summary make sure various basic java options work
 * @library /test/lib
 *
 * @run driver sanity.VerifyFlagConstraintsTests
 */
public class VerifyFlagConstraintsTests {
    public static void main(String[] args) throws Exception {
        FlagConstraintTester.create().run("-XX:+UseG1GC",
                        "-XX:MaxGCPauseMillis=18446744073709551614",
                        "-XX:GCPauseIntervalMillis=0",
                        "-XX:AllocatePrefetchDistance=-1",
                        "-XX:AllocatePrefetchStepSize=29",
                        "-XX:AllocatePrefetchStyle=3")
                .testEq("MaxGCPauseMillis", "1")
                .testEq("GCPauseIntervalMillis", "2")
                .testEq("AllocatePrefetchDistance", "0")
                .testEq("AllocatePrefetchStepSize", "24");

        FlagConstraintTester.create().run(" -XX:CompileThreshold=268435456",
                "-XX:OnStackReplacePercentage=40").testEq("CompileThreshold", "268435455");

        FlagConstraintTester.create().run(" -XX:CompileThreshold=268435456")
                .testEq("CompileThreshold", "268435455")
                .testEq("OnStackReplacePercentage", "34");
    }

    static class FlagConstraintTester {
        private String[] enableArgs = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+VerifyFlagConstraints"};
        private Map<String, String> verifiedContents = new HashMap<>();

        public static FlagConstraintTester create() {
            return new FlagConstraintTester();
        }

        public FlagConstraintTester run(String... flags) throws Exception {
            String[] cmds = new String[flags.length + 2];
            cmds[0] = enableArgs[0];
            cmds[1] = enableArgs[1];
            for (int i = 0; i < flags.length; i++) {
                cmds[i + 2] = flags[i];
            }
            OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(cmds);
            outputAnalyzer.shouldHaveExitValue(0);
            System.out.println(outputAnalyzer.getOutput());
            for (String line : outputAnalyzer.asLines()) {
                String[] rets = line.split(":");
                if (rets.length != 2) {
                    throw new RuntimeException("Expect 2 entries in the result of flag verifying, but is " + line);
                }
                verifiedContents.put(rets[0], rets[1]);
            }
            return this;
        }

        public FlagConstraintTester testEq(String param, String expected) {
            Asserts.assertEquals(expected, verifiedContents.get(param));
            return this;
        }
    }
}
