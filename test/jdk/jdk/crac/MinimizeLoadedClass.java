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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;

/*
 * @test
 * @requires (os.family == "linux")
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @summary Test only load 3 classes when crac not enabled.
 * @library /test/lib
 * @build TestHello
 * @run main/othervm MinimizeLoadedClass
 */
public class MinimizeLoadedClass {
    private static final String[] MIN_CLASS_SET = new String[]{
            "jdk.crac.Configuration",
            "jdk.crac.Resource",
            "jdk.internal.crac.JDKResource"
    };

    private static final String[] MORE_CLASS_SET = new String[]{
            "jdk.internal.crac.Core",
            "jdk.crac.Context",
            "jdk.crac.impl.AbstractContextImpl",
            "jdk.internal.crac.JDKContext",
            "jdk.internal.crac.JDKContext$ContextComparator",
            "jdk.crac.Core",
            "jdk.crac.impl.OrderedContext",
            "jdk.crac.impl.OrderedContext$ContextComparator"
    };

    public static void main(String[] args) throws IOException {
        runTest(new String[]{"-verbose:class","TestHello"}, new String[][]{MIN_CLASS_SET}, new String[][]{MORE_CLASS_SET});
        runTest(new String[]{"-verbose:class", "-XX:CRaCCheckpointTo=cr","TestHello"}, new String[][]{MIN_CLASS_SET, MORE_CLASS_SET}, new String[0][0]);
    }

    private static void runTest(String[] cmds, String[][] includes, String[][] excludes) throws IOException {
        ProcessBuilder pb = ProcessTools.createTestJvm(cmds);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        for (String[] include : includes) {
            for (String str : include) {
                output.shouldContain(str);
            }
        }
        for (String[] exclude : excludes) {
            for (String str : exclude) {
                output.shouldNotContain(str);
            }
        }
    }
}
