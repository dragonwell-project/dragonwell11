package jdk.internal.misc;

public interface JavaAppClassLoaderAccess {

    /**
     * append path to app clasloader's classpath.
     * @param path
     */
    void appendToClassPath(String path);
}
