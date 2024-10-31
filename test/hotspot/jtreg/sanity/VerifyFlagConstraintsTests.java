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

import java.util.HashMap;
import java.util.Map;

/*
 * @test
 * @summary make sure various basic java options work
 * @library /test/lib
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
               /* .testEq("MaxGCPauseMillis", "1")
                .testEq("GCPauseIntervalMillis", "2")
                .testEq("AllocatePrefetchDistance", "8")
                .testEq("AllocatePrefetchStepSize", "24")*/
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:InteriorEntryAlignment=99",
                        "-XX:OptoLoopAlignment=9")
                /*.testEq("InteriorEntryAlignment", "32")
                .testEq("OptoLoopAlignment", "8")*/
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:CompileThreshold=268435456")
               // .testEq("CompileThreshold", "268435455")
               // .testEq("OnStackReplacePercentage", "34")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:ObjectAlignmentInBytes=99")
                //.testEq("ObjectAlignmentInBytes", "64")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:TypeProfileLevel=123")
               // .testEq("TypeProfileLevel", "122")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:TypeProfileLevel=333")
                //.testEq("TypeProfileLevel", "222")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:BiasedLockingBulkRebiasThreshold=250",
                        "-XX:BiasedLockingBulkRevokeThreshold=249",
                        "-XX:BiasedLockingDecayTime=2500")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:BiasedLockingStartupDelay=99")
              //  .testEq("BiasedLockingStartupDelay", "90")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:YoungPLABSize=128")
               // .testEq("YoungPLABSize", "256")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:YoungPLABSize=9999999")
                //.testEq("YoungPLABSize", "65536")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:ThreadStackSize=128")
                //.testEq("ThreadStackSize", "136")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:ReservedCodeCacheSize=4096")
                //.testEq("ReservedCodeCacheSize", "2555904")
                .testWithSuggested();

        FlagConstraintTester.create().run("-XX:MarkStackSize=0")
                //.testEq("ReservedCodeCacheSize", "2555904")
                .testWithSuggested();
    }

    static class FlagConstraintTester {
        private static String[] enableArgs = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+VerifyFlagConstraints"};
        private Map<String, String> suggestedArgs = new HashMap<>();
        private Map<String, String> inputArgs = new HashMap<>();

        public static FlagConstraintTester create() {
            return new FlagConstraintTester();
        }

        public FlagConstraintTester run(String... flags) throws Exception {
            OutputAnalyzer outputAnalyzer = doRun(flags, true);
            extractSuggestions(outputAnalyzer);
            return this;
        }

        private OutputAnalyzer doRun(String[] flags, boolean updateInputs) throws Exception {
            String[] cmds = new String[flags.length + 2];
            cmds[0] = enableArgs[0];
            cmds[1] = enableArgs[1];
            for (int i = 0; i < flags.length; i++) {
                cmds[i + 2] = flags[i];
                if (updateInputs) {
                    String[] values = flags[i].split("=");
                    String key;
                    String value;
                    if (values.length == 1) {
                        // case of -XX:+SomeOpt
                        key = values[0].substring(5);
                        value = String.valueOf(values[0].charAt(4));
                    } else {
                        key = values[0].substring(4);
                        value = values[1];
                    }
                    inputArgs.put(key, value);
                }
            }
            OutputAnalyzer outputAnalyzer = ProcessTools.executeTestJvm(cmds);
            System.out.println(outputAnalyzer.getOutput());
            outputAnalyzer.shouldHaveExitValue(0);
            return outputAnalyzer;
        }

        private void extractSuggestions(OutputAnalyzer outputAnalyzer) {
            for (String line : outputAnalyzer.asLines()) {
                String[] rets = line.split(":");
                if (rets.length != 2) {
                    throw new RuntimeException("Expect 2 entries in the result of flag verifying, but is " + line);
                }
                suggestedArgs.put(rets[0], rets[1]);
            }
        }

        /**
         * Start JVM with suggested flags again. This time there should be no suggestion anymore.
         *
         * @throws Exception
         */
        public void testWithSuggested() throws Exception {
            suggestedArgs.entrySet().forEach(suggested ->
                    inputArgs.put(suggested.getKey(), suggested.getValue())
            );
            String[] args = inputArgs.entrySet().stream().map(entry -> {
                if (entry.getValue().equals("-") || entry.getValue().equalsIgnoreCase("false")) {
                    return "-XX:-" + entry.getKey();
                }
                if (entry.getValue().equals("+") || entry.getValue().equalsIgnoreCase("true")) {
                    return "-XX:+" + entry.getKey();
                }
                return "-XX:" + entry.getKey() + "=" + entry.getValue();
            }).toArray(String[]::new);
            OutputAnalyzer outputAnalyzer = doRun(args, false);
            outputAnalyzer.shouldBeEmpty();
        }

        public FlagConstraintTester testEq(String param, String expected) {
            Asserts.assertEquals(expected, suggestedArgs.get(param));
            return this;
        }
    }
}
