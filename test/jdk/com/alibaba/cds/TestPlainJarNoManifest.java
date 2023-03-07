/**
 * @test
 * @summary far jar and some jar have no manifest.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestPlainJarNoManifest
 */
public class TestPlainJarNoManifest implements SingleProjectProvider {
    public static void main(String[] args) throws Exception {
        new EagerAppCDSTestRunner().run(new TestPlainJarNoManifest());
    }

    @Override
    public Project getProject() {
        return project;
    }

    private JavaSource[] fooSource = new JavaSource[]{
            new JavaSource(
                    "com.x.Add", "public class Add",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("add",
                                    "public int add(int a,int b) { return a+b; } ")
                    }
            ),
            new JavaSource(
                    "com.y.Sub", "public class Sub",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("sub",
                                    "public int sub(int a,int b) {return a-b;}")
                    }
            ),
            new JavaSource(
                    "com.z.Main", "public class Main",
                    new String[]{"com.x.Add", "com.y.Sub", "com.m.Multiply", "com.u.Divide"}, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("main",
                                    "public static void main(String[] args) {" +
                                            "Add add = new Add();" +
                                            "System.out.println(add.add(10,20));" +
                                            "Sub sub = new Sub();" +
                                            "System.out.println(sub.sub(100,10));" +
                                            "Multiply m = new Multiply();" +
                                            "System.out.println(m.multiply(4,12));" +
                                            "}"
                            )
                    }
            )
    };

    private JavaSource[] barSource = new JavaSource[]{
            new JavaSource(
                    "com.m.Multiply", "public class Multiply",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("multiply",
                                    "public int multiply(int a,int b) { return a*b; } ")
                    }
            ),
            new JavaSource(
                    "com.u.Divide", "public class Divide",
                    null, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("divide",
                                    "public int divide(int a,int b) {return a/b;}")
                    }
            )
    };

    private Project project = new Project(new RunWithURLClassLoaderConf("com.z.Main"),
            new Artifact[]{
                    Artifact.createPlainJar("foo", "foo-lib", "add-sub.1.0.jar", new String[]{"bar"}, fooSource,
                            new ArtifactOption[]{ArtifactOption.NO_MANIFEST, ArtifactOption.LOAD_BY_URLCLASSLOADER}),
                    Artifact.createPlainJar("bar", "bar-lib", "mul-div-1.0.jar", null, barSource,
                            new ArtifactOption[]{ArtifactOption.LOAD_BY_URLCLASSLOADER})
            },
            new ExpectOutput(new String[]{"30", "90", "48",
                    "Successful loading of class com/m/Multiply",
                    "Successful loading of class com/x/Add",
                    "Successful loading of class com/y/Sub"
            }));
}
