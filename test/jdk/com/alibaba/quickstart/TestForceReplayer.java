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
 * @summary Test -Xquickstart:replay option
 * @library /test/lib
 * @requires os.arch=="amd64"
 * @run main/othervm/timeout=600 TestForceReplayer
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestForceReplayer {
    private static String cachepath = System.getProperty("user.dir");
    public static void main(String[] args) throws Exception {
        TestForceReplayer test = new TestForceReplayer();
        cachepath = cachepath + "/dir-does-not-exist";
        test.runWithoutCache();
    }

    void runWithoutCache() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xquickstart:path=" + cachepath, "-Xquickstart:replay,verbose" + Config.QUICKSTART_FEATURE_COMMA, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println(output.getStdout());
        output.shouldContain("QuickStart replay role is specified without shared directory found. Running as a normal process with quickstart disabled.");
        output.shouldHaveExitValue(0);
    }
}

