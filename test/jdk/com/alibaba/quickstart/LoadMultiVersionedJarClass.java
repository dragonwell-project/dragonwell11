import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

public class LoadMultiVersionedJarClass {
    private static final String MULTI_VERSIONED_JAR = "multi-release.jar";

    public static void main(String... args) throws Exception {
        //  class loader with name
        testURLClassLoader();
    }

    public static void testURLClassLoader() throws Exception {
        URL[] urls = new URL[] { new File(MULTI_VERSIONED_JAR).toPath().toUri().toURL() };
        ClassLoader parent = ClassLoader.getSystemClassLoader();
        MyWebAppClassLoader loader = new MyWebAppClassLoader(urls, parent);

        try {
            Method m = Class.forName("com.alibaba.util.Utils").getDeclaredMethod("registerClassLoader",
                                     ClassLoader.class, String.class);
            m.setAccessible(true);
            m.invoke(null, loader, "myloader");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        Class<?> c = Class.forName("version.PackagePrivate", true, loader);
        System.out.println("loaded: " + c);
    }

}

