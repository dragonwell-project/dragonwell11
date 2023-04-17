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
 * @summary far jar and some jar have no manifest.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestFatJarNoManifest
 */
public class TestFatJarNoManifest implements SingleProjectProvider {
    public static void main(String[] args) throws Exception {
        new EagerAppCDSTestRunner().run(new TestFatJarNoManifest());
    }

    @Override
    public Project getProject() {
        return project;
    }

    private JavaSource[] aSources = new JavaSource[]{
            new JavaSource("com.a.A1", "public class A1", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"A1\"; }")
            }),
            new JavaSource("com.a.A2", "public class A2", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"A2\"; }")
            })
    };

    private JavaSource[] bSources = new JavaSource[]{
            new JavaSource("com.b.B1", "public class B1", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"B1\"; }")
            }),
            new JavaSource("com.b.B2", "public class B2", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"B2\"; }")
            })
    };
    private JavaSource[] cSources = new JavaSource[]{
            new JavaSource("com.c.C1", "public class C1", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"C1\"; }")
            }),
            new JavaSource("com.c.C2", "public class C2", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"C2\"; }")
            }),
            new JavaSource("com.m.M1", "public class M1", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"M1\"; }")
            }),
            new JavaSource("com.m.M2", "public class M2", null, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("name", "public String name() { return \"M2\"; }")
            }),
            new JavaSource("com.m.Main", "public class Main", new String[]{"com.a.*", "com.b.*"}, null, new JavaSource.MethodDesc[]{
                    new JavaSource.MethodDesc("main", "    public static void main(String[] args) {\n" +
                            "        M1 m1 = new M1();\n" +
                            "        System.out.println(m1.name());\n" +
                            "        M2 m2 = new M2();\n" +
                            "        System.out.println(m2.name());\n" +
                            "        System.out.println(new B1().name());" +
                            "        System.out.println(new A2().name());" +
                            "    }")
            })
    };

    private Project project = new Project(new RunFatJarConf("com.m.Main"),
            new Artifact[]{
                    Artifact.createFatJar("all-in-one", "fat-lib", "m-fat.jar",
                            new Artifact[]{
                                    Artifact.createPlainJar("a", "lib", "a.jar", null, aSources,new ArtifactOption[]{ArtifactOption.NO_MANIFEST}),
                                    Artifact.createPlainJar("b", "lib", "b.jar", null, bSources,new ArtifactOption[]{ArtifactOption.NO_MANIFEST}),
                                    Artifact.createClasses("c", "myclasses", new String[]{"a", "b"}, cSources)
                            })
            },
            new ExpectOutput(new String[]{"M1", "M2", "B1", "A2"}));
}
