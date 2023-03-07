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

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;

public class CompileUtil {
    private static final boolean PRINT_SOURCE = true;

    public static void compile(JavaSource[] javaSources, File classesDir, List<String> options) throws IOException {
        Map<String, JavaByteObject> byteObjectMap = compileInMemory(javaSources, options);
        writeToFile(javaSources, byteObjectMap, classesDir);
    }

    private static void writeToFile(JavaSource[] javaSources, Map<String, JavaByteObject> byteObjectMap, File classesDir) throws IOException {
        Set<String> writtenSet = new HashSet<>();
        for (JavaSource javaSource : javaSources) {
            JavaByteObject jbo = byteObjectMap.get(javaSource.getClassName());
            byte[] classBytes = jbo.getBytes();
            if (null == classBytes || classBytes.length == 0) {
                throw new RuntimeException("Class : " + javaSource.getClassName() + " compile failed!");
            }
            doWrite(classesDir, javaSource, jbo);
            writtenSet.add(javaSource.getClassName());
        }

        //inner classes
        for (Map.Entry<String, JavaByteObject> entry : byteObjectMap.entrySet()) {
            if (!writtenSet.contains(entry.getKey())) {
                doWrite(classesDir, entry.getKey(), entry.getValue());
            }
        }
    }

    private static void doWrite(File classesDir, JavaSource javaSource, JavaByteObject jbo) throws IOException {
        String destFile;
        if (javaSource.getPackageName() != null) {
            preparePackageDir(classesDir, javaSource.getPackageName());
            destFile = javaSource.getPackageName().replace('.', File.separatorChar) + File.separator + javaSource.getName() + ".class";
        } else {
            destFile = javaSource.getName() + ".class";
        }
        try (FileOutputStream fos = new FileOutputStream(new File(classesDir, destFile))) {
            fos.write(jbo.getBytes());
        }
    }

    private static void doWrite(File classesDir, String className, JavaByteObject jbo) throws IOException {
        String destFile;
        int idx = className.lastIndexOf('.');
        if (idx != -1) {
            String packageName = className.substring(0, idx);
            preparePackageDir(classesDir, className.substring(0, idx));
            destFile = packageName.replace('.', File.separatorChar) + File.separator + className.substring(idx + 1) + ".class";
        } else {
            destFile = className + ".class";
        }
        try (FileOutputStream fos = new FileOutputStream(new File(classesDir, destFile))) {
            fos.write(jbo.getBytes());
        }
    }

    private static void preparePackageDir(File classesDir, String packageName) {
        File f = new File(classesDir, packageName.replace('.', File.separatorChar));
        if (!f.exists() && !f.mkdirs()) {
            throw new RuntimeException("Prepare package dir :" + f + " failed!Classes Dir: " + classesDir + ",package name: " + packageName);
        }
    }

    private static Map<String, JavaByteObject> compileInMemory(JavaSource[] javaSources, List<String> options) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<>();

        final Map<String, JavaByteObject> byteObjectMap = new HashMap<>();
        for (JavaSource javaSource : javaSources) {
            byteObjectMap.put(javaSource.getClassName(), new JavaByteObject(javaSource.getClassName()));
        }

        StandardJavaFileManager standardFileManager =
                compiler.getStandardFileManager(diagnostics, null, null);

        JavaFileManager fileManager = createFileManager(standardFileManager,
                byteObjectMap);
        JavaCompiler.CompilationTask task = compiler.getTask(null,
                fileManager, diagnostics, options, null, getCompilationUnits(javaSources));

        if (!task.call()) {
            diagnostics.getDiagnostics().forEach(System.out::println);
        }
        fileManager.close();
        return byteObjectMap;
    }

    public static Iterable<? extends JavaFileObject> getCompilationUnits(JavaSource[] javaSources) {
        List<JavaFileObject> fileObjectList = new ArrayList<>();
        for (JavaSource src : javaSources) {
            fileObjectList.add(new JavaStringObject(src.getClassName(), src.buildJavaSourceCode()));
            if (PRINT_SOURCE) {
                System.out.println("================= " + src.getClassName() + "=================");
                System.out.println(src.buildJavaSourceCode());
            }
        }
        return fileObjectList;
    }


    private static JavaFileManager createFileManager(StandardJavaFileManager fileManager,
                                                     Map<String, JavaByteObject> byteObjectMap) {
        return new ForwardingJavaFileManager<StandardJavaFileManager>(fileManager) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location,
                                                       String className, JavaFileObject.Kind kind,
                                                       FileObject sibling) throws IOException {
                JavaFileObject javaFileObject = byteObjectMap.get(className);
                if (null == javaFileObject) {
                    javaFileObject = new JavaByteObject(className);
                    byteObjectMap.put(className, (JavaByteObject) javaFileObject);
                }
                return javaFileObject;
            }
        };
    }


    private static class JavaByteObject extends SimpleJavaFileObject {
        private ByteArrayOutputStream outputStream;

        protected JavaByteObject(String name) {
            super(URI.create("bytes:///" + name + name.replaceAll("\\.", "/")), Kind.CLASS);
            outputStream = new ByteArrayOutputStream();
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return outputStream;
        }

        public byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }

    public static class JavaStringObject extends SimpleJavaFileObject {
        private final String source;

        protected JavaStringObject(String name, String source) {
            super(URI.create("string:///" + name.replaceAll("\\.", "/") +
                    Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors)
                throws IOException {
            return source;
        }
    }
}
