/*
 * Copyright (c) 2024, Alibaba Group Holding Limited. All rights reserved.
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
import jdk.crac.*;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracProcess;
import jdk.test.lib.crac.CracTest;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary test if pipe can restore
 * @library /test/lib
 * @build RestorePipeFdTest
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest
 */
public class RestorePipeFdTest implements CracTest{
    private static final String MSG_1 = "[before checkpointRestore]this is an message from stdout";
    private static final String MSG_2 = "[before checkpointRestore]this is an message from stderr";
    private static final String MSG_3 = "[after checkpointRestore]this is an message from stdout";
    private static final String MSG_4 = "[after checkpointRestore]this is an message from stderr";

    @Override
    public void test() throws Exception {
        CracBuilder cracBuilder = new CracBuilder().captureOutput(true)
                                          .restorePipeStdOutErr(true);
        CracProcess crProcess = cracBuilder.startCheckpoint();
        crProcess.waitForCheckpointed();
        OutputAnalyzer crOutputAnalyzer = crProcess.outputAnalyzer();
        crOutputAnalyzer.shouldContain(MSG_1);
        crOutputAnalyzer.shouldContain(MSG_1);

        CracProcess restoreProcess = cracBuilder.doRestore();
        OutputAnalyzer restoreOutputAnalyzer = restoreProcess.outputAnalyzer();
        restoreOutputAnalyzer.shouldContain(MSG_3);
        restoreOutputAnalyzer.shouldContain(MSG_4);
    }

    @Override
    public void exec() throws Exception {
        System.out.println(MSG_1);
        System.err.println(MSG_2);
        Core.checkpointRestore();
        System.out.println(MSG_3);
        System.err.println(MSG_4);
    }
}
