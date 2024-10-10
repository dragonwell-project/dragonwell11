package jdk.test.lib.crac;

import jdk.crac.Core;

import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static jdk.test.lib.Asserts.*;

/**
 * CRaC tests usually consists of two parts; the test started by JTreg through the 'run' tag
 * and subprocesses started by the test with various VM options. These are represented by the
 * {@link #test()} and {@link #exec()} methods.
 * CracTest use '@run driver jdk.test.crac.lib.CracTest' as the executable command; the main
 * method in this class discovers the executed class from system properties passed by JTReg,
 * instantiates the test (public no-arg constructor is needed), populates fields annotated
 * with {@link CracTestArg} and executes the {@link #test()} method.
 * The test method is expected to use {@link CracBuilder} to start another process. By default,
 * CracBuilder invokes the test with arguments that will again instantiate and fill the instance
 * and invoke the {@link #exec()} method.
 */
public interface CracTest {

    String RESTORED_MESSAGE = "Restored";

    /**
     * This method is called when JTReg invokes the test; it is supposed to start
     * another process (most often using CRaC VM options) and validate its behaviour.
     *
     * @throws Exception
     */
    void test() throws Exception;

    /**
     * This method is invoked in the subprocess; this is where you're likely to call
     * {@link Core#checkpointRestore()}.
     *
     * @throws Exception
     */
    void exec() throws Exception;

    class ArgsHolder {
        private static final String RUN_TEST = "__run_test__";
        private static Class<? extends CracTest> testClass;
        private static String[] args;
        // This field is present as workaround for @build <test> somehow missing
        // the annotation when
        private static final Class<CracTestArg> dummyField = CracTestArg.class;
    }

    /**
     * Main method for orchestrating the test. This should be called directly by JTReg.
     */
    static void main(String[] args) throws Exception {
        String testClassName;
        if (args.length == 0 || !ArgsHolder.RUN_TEST.equals(args[0])) {
            // We will look up the class name (and package) to avoid boilerplate in any @run invocation
            String testFile = System.getProperty("test.file");
            String source = Files.readString(Path.of(testFile)).replace('\n', ' ');
            Matcher clsMatcher = Pattern.compile("class\\s+(\\S+)\\s+(extends\\s+\\S+\\s+)?implements\\s+(\\S+\\s*,\\s*)*CracTest").matcher(source);
            if (!clsMatcher.find()) {
                fail("Cannot find test class in " + testFile + ", does it look like class <test> implements CracTest?");
            }
            testClassName = clsMatcher.group(1);
            Matcher pkgMatcher = Pattern.compile("package\\s+([^;]+);").matcher(source);
            if (pkgMatcher.find()) {
                testClassName = pkgMatcher.group(1) + "." + testClassName;
            }
        } else {
            testClassName = args[1];
        }

        // When we use CracTest as driver the file with test is not compiled without a @build tag.
        // We could compile the class here and load it from a new classloader but since the test library
        // is not compiled completely we could be missing some dependencies - this would be just too fragile.
        Class<?> testClass;
        try {
            testClass = Class.forName(testClassName);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Test class " + testClassName + " not found, add jtreg tag @build " + args[0], e);
        }
        if (CracTest.class.isAssignableFrom(testClass)) {
            //noinspection unchecked
            run((Class<? extends CracTest>) testClass, args);
        } else {
            throw new IllegalArgumentException("Class " + testClass.getName() + " does not implement CracTest!");
        }
    }

    /**
     * This method should be invoked from the public static void main(String[]) method.
     *
     * @param testClass Class implementing the test.
     * @param args Arguments received in the main method.
     * @throws Exception
     */
    static void run(Class<? extends CracTest> testClass, String[] args) throws Exception {
        assertNotNull(args);
        ArgsHolder.testClass = testClass;
        int argsOffset = 0;
        if (args.length == 0 || !args[0].equals(ArgsHolder.RUN_TEST)) {
            String[] newArgs = new String[args.length + 2];
            newArgs[0] = ArgsHolder.RUN_TEST;
            newArgs[1] = testClass.getName();
            System.arraycopy(args, 0, newArgs, 2, args.length);
            ArgsHolder.args = newArgs;
        } else {
            argsOffset = 2;
        }

        try {
            Constructor<? extends CracTest> ctor = testClass.getConstructor();
            CracTest testInstance = ctor.newInstance();
            Field[] argFields = getArgFields(testClass);
            for (int index = 0; index < argFields.length; index++) {
                Field f = argFields[index];
                assertFalse(Modifier.isFinal(f.getModifiers()), "@CracTestArg fields must not be final!");
                Class<?> t = f.getType();
                if (index + argsOffset >= args.length) {
                    if (f.getAnnotation(CracTestArg.class).optional()) {
                        break;
                    } else {
                        fail("Not enough args for field " + f.getName() + "(" + index + "): have " + (args.length - argsOffset));
                    }
                }
                String arg = args[index + argsOffset];
                Object value = arg;
                if (t == boolean.class || t == Boolean.class) {
                    assertTrue("true".equals(arg) || "false".equals(arg), "Argument " + index + "Boolean arg should be either 'true' or 'false', was: " + arg);
                    value = Boolean.parseBoolean(arg);
                } else if (t == int.class || t == Integer.class) {
                    try {
                        value = Integer.parseInt(arg);
                    } catch (NumberFormatException e) {
                        fail("Cannot parse argument '" + arg + "' as integer for @CracTestArg(" + index + ") " + f.getName());
                    }
                } else if (t == long.class || t == Long.class) {
                    try {
                        value = Long.parseLong(arg);
                    } catch (NumberFormatException e) {
                        fail("Cannot parse argument '" + arg + "' as long for @CracTestArg(" + index + ") " + f.getName());
                    }
                } else if (t.isEnum()) {
                    value = Enum.valueOf((Class<Enum>) t, arg);
                }
                f.setAccessible(true);
                f.set(testInstance, value);
            }
            if (argsOffset == 0) {
                testInstance.test();
            } else {
                testInstance.exec();
            }
        } catch (NoSuchMethodException e) {
            fail("Test class " + testClass.getName() + " is expected to have a public no-arg constructor");
        }
    }

    private static Field[] getArgFields(Class<? extends CracTest> testClass) {
        // TODO: check superclasses
        Field[] sortedFields = Stream.of(testClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(CracTestArg.class))
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(CracTestArg.class).value()))
                .toArray(Field[]::new);
        if (sortedFields.length == 0) {
            return sortedFields;
        }
        int firstOptional = -1;
        for (int i = 0; i < sortedFields.length; ++i) {
            CracTestArg annotation = sortedFields[i].getAnnotation(CracTestArg.class);
            int index = annotation.value();
            assertGreaterThanOrEqual(index, 0);
            if (i == 0) {
                assertEquals(0, index, "@CracTestArg numbers should start with 0");
            }
            if (index < i) {
                fail("Duplicate @CracTestArg(" + index + "): both fields " + sortedFields[i - 1].getName() + " and " + sortedFields[i].getName());
            } else if (index > i) {
                fail("Gap in @CracTestArg indices: missing " + i + ", next is " + index);
            }
            if (annotation.optional()) {
                firstOptional = index;
            } else if (firstOptional >= 0) {
                fail("Argument " + firstOptional + " is optional; all subsequent arguments must be optional, too.");
            }
        }
        return sortedFields;
    }

    /**
     * Used as argument for {@link CracBuilder#args(String...)}.
     */
    static String[] args(String... extraArgs) {
        assertNotNull(ArgsHolder.args, "Args are null; are you trying to access them from test method?");
        if (extraArgs == null || extraArgs.length == 0) {
            return ArgsHolder.args;
        } else {
            return Stream.concat(Stream.of(ArgsHolder.args), Stream.of(extraArgs)).toArray(String[]::new);
        }
    }

    static Class<? extends CracTest> testClass() {
        return ArgsHolder.testClass;
    }
}

