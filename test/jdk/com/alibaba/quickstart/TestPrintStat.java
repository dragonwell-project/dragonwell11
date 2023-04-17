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

/*
 * @test
 * @summary Test -Xquickstart:printStat option
 * @library /test/lib
 * @requires os.arch=="amd64"
 * @run main/othervm/timeout=600 TestPrintStat
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestPrintStat {
    private static String cachepath = System.getProperty("user.dir");
    public static void main(String[] args) throws Exception {
        TestPrintStat test = new TestPrintStat();
        cachepath = cachepath + "/printstat";
        test.runWithoutCache();
        test.runAsTracer();
        test.runAsReplayerWithPrintStat();
    }

    void runWithoutCache() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:printStat" + Config.QUICKSTART_FEATURE_COMMA, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("There is no cache in " + cachepath);
        output.shouldHaveExitValue(0);
    }

    void runAsTracer() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:verbose" + Config.QUICKSTART_FEATURE_COMMA, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Running as tracer");
        output.shouldHaveExitValue(0);
    }

    void runAsReplayerWithPrintStat() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:printStat" + Config.QUICKSTART_FEATURE_COMMA, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Current statistics for cache " + cachepath);
        output.shouldHaveExitValue(0);
    }
}

