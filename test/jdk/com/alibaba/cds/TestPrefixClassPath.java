import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @test
 * @summary Test the jar when dumping is subset of jars when replaying
 * @library /test/lib /com/alibaba/lib
 * @requires os.arch=="amd64"
 * @run main/othervm TestPrefixClassPath
 */
public class TestPrefixClassPath implements SingleProjectProvider {

    private static final String NO_USED_JAR_1 = "foo1.jar";
    private static final String NO_USED_JAR_2 = "foo2.jar";
    public static void main(String[] args) throws Exception {
        new ClassPathOrderChangedTestRunner(
                cps -> {
                    List<String> newcps = new ArrayList<>(cps);
                    Comparator<String> comp = String::compareTo;
                    newcps.sort(comp);
                    //remove no need jar.
                    Iterator<String> iter = newcps.iterator();
                    while (iter.hasNext()) {
                        String jar = iter.next();
                        if (jar.endsWith(NO_USED_JAR_1) || jar.endsWith(NO_USED_JAR_2)) {
                            iter.remove();
                        }
                    }
                    return newcps;
                },
                cps -> {
                    List<String> newcps = new ArrayList<>(cps);
                    Comparator<String> comp = String::compareTo;
                    newcps.sort(comp.reversed());
                    return newcps;
                }
        ).run(new TestPrefixClassPath());
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
                    new String[]{"com.x.Add", "com.y.Sub"}, null,
                    new JavaSource.MethodDesc[]{
                            new JavaSource.MethodDesc("main",
                                    "public static void main(String[] args) {" +
                                            "Add add = new Add();" +
                                            "System.out.println(add.add(10,20));" +
                                            "Sub sub = new Sub();" +
                                            "System.out.println(sub.sub(100,10));" +
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

    private Project project = new Project(new RunMainClassConf("com.z.Main"),
            new Artifact[]{
                    Artifact.createPlainJar("foo3", "foo3", "foo3.jar", null, fooSource),
                    Artifact.createPlainJar("foo2", "foo2", NO_USED_JAR_2, null, barSource),
                    Artifact.createPlainJar("foo1", "foo1", NO_USED_JAR_1, null, barSource)
            },
            new ExpectOutput(new String[]{"30", "90"
            }));
}
