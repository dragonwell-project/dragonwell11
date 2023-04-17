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

/**
 * @test
 * @summary test main class with parameter like a VM option.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestEagerAppCDSWithParametersProfileDump
 */
public class TestEagerAppCDSWithParametersProfileDump implements SingleProjectProvider {
    public static void main(String[] args) throws Exception {
        new ProfileDumpTestRunner(QuickStartFeature.EAGER_APPCDS).run(new TestEagerAppCDSWithParametersProfileDump());
    }

    @Override
    public Project getProject() {
        return project;
    }

    private Project project = new Project(new RunMainClassConf("com.z.Main",new String[]{"-XX:+ArbitraryOptionLikeVMOption"}),
            new Artifact[]{
                    Artifact.createPlainJar("foo", "foo-lib", "add-sub.1.0.jar", new String[]{"bar"}, SourceConstants.ADD_SUB_SOURCE),
                    Artifact.createPlainJar("bar", "bar-lib", "mul-div-1.0.jar", null, SourceConstants.MUL_DIV_SOURCE)
            },
            new ExpectOutput(new String[]{"30", "90", "48"
            }));
}
