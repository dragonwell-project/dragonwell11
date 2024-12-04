/*
 * Copyright (c) 2025, Alibaba Group Holding Limited. All rights reserved.
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
 * @test TestPolymorphicInlining
 * @summary invoke tripple morphic method and see if either one is inlined.
 * @library /test/lib
 * @run main TestPolymorphicInlining
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestPolymorphicInlining {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb1 = ProcessTools.createJavaProcessBuilder(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+PrintInlining",
                "-XX:+PolymorphicInlining",
                Test.class.getName());
        OutputAnalyzer output1 = new OutputAnalyzer(pb1.start());
        output1.shouldMatch("TestPolymorphicInlining\\$Test\\$Child\\d::foo.*inline");
        output1.shouldHaveExitValue(0);

        ProcessBuilder pb2 = ProcessTools.createJavaProcessBuilder(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+PrintInlining",
                "-XX:-PolymorphicInlining",
                Test.class.getName());
        OutputAnalyzer output2 = new OutputAnalyzer(pb2.start());
        output2.shouldNotContain("TestPolymorphicInlining$Test$Child");
        output2.shouldHaveExitValue(0);
    }
    static class Test {
        private final int x = -1;
        private static int n = 1000000;
        private final Parent c1 = new Child1();
        private final Parent c2 = new Child2();
        private final Parent c3 = new Child3();

        class Parent {
            int foo() {
                return x;
            }
        }

        class Child1 extends Parent {
            int foo() {
                return x;
            }
        }

        class Child2 extends Parent {
            int foo() {
                return x;
            }
        }

        class Child3 extends Parent {
            int foo() {
                return x;
            }
        }

        int invoke(Parent p) {
            return p.foo();
        }

        public static void main(String[] args) throws Exception {
            Test test = new Test();
            int x = 0;
            while (n-- > 0) {
                x += test.invoke(test.c1) + test.invoke(test.c2) + test.invoke(test.c3);
            }
        }
    }
}
