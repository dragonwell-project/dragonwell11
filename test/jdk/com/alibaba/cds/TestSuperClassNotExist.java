import java.io.File;

/**
 * @test
 * @summary The class 'jdk.jfr.Event' and its subclass handle special in jvm, it's source is null.This case test if sat-adapter can skip these classes.
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64" | os.arch=="aarch64"
 * @run main/othervm TestSuperClassNotExist
 */
public class TestSuperClassNotExist implements SingleProjectProvider {
    public static void main(String[] args) throws Exception {
        new EagerAppCDSTestRunner().run(new TestSuperClassNotExist());
    }

    @Override
    public Project getProject() {
        return project;
    }

    private JavaSource[] fooSource = new JavaSource[]{
            new JavaSource(
                    "com.x.MyJfrEvent", "public class MyJfrEvent extends jdk.jfr.Event",
                    new String[]{"jdk.jfr.Event"}, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("foo",
                                    "public int foo() { return 1; } ")
                    }
            ),
            new JavaSource(
                    "com.z.Main", "public class Main",
                    new String[]{"com.x.MyJfrEvent"}, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("main",
                                    "public static void main(String[] args) {" +
                                            "MyJfrEvent event = new MyJfrEvent();" +
                                            "System.out.println(event);" +
                                            "}"
                            )
                    }
            )
    };

    private Project project = new Project(new RunWithURLClassLoaderConf("com.z.Main"),
            new Artifact[]{
                    Artifact.createSignPlainJar("foo", "foo-lib", "a.1.0.jar", null, fooSource,
                            new ArtifactOption[]{ArtifactOption.NO_MANIFEST, ArtifactOption.LOAD_BY_URLCLASSLOADER},null)
            },


            //jdk.jfr.Event and it's subclasses should not dump into cds.
            new ExpectOutput(new String[]{
                    "Successful loading of class com/z/Main",
            }, new String[]{
                    "Successful loading of class jdk/jfr/Event",
                    "Successful loading of class com/x/MyJfrEvent",
            }));
}
