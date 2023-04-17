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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The custom class loader extend URLClassLoader,but URLs not provide by constructor.
 */
public class EmptyURLsURLClassLoadLauncher extends URLClassLoader {
    private URL[] jarFileUrls;
    private Set<String>[] classes;
    private JarFile[] jarFiles;

    public EmptyURLsURLClassLoadLauncher(String name, String[] files, URL[] urls, ClassLoader parent) throws IOException {
        super(name, urls, parent);
        this.jarFileUrls = new URL[files.length];
        this.classes = new HashSet[files.length];
        this.jarFiles = new JarFile[files.length];
        for (int i = 0; i < files.length; i++) {
            jarFileUrls[i] = new File(files[i]).toURI().toURL();
            jarFiles[i] = new JarFile(files[i]);
            classes[i] = readClasses(jarFiles[i]);
        }
    }

    private Set<String> readClasses(JarFile jarFile) {
        Set<String> classes = new HashSet<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                classes.add(jarEntry.getName());
            }
        }
        return classes;
    }

    public static void main(String[] args) throws Exception {
        String mainClass = args[0];
        String[] files = new String[args.length - 1];
        System.arraycopy(args, 1, files, 0, args.length - 1);

        EmptyURLsURLClassLoadLauncher uccl = new EmptyURLsURLClassLoadLauncher("EmptyURLsURLClassLoadLauncher", files, new URL[0], null);
        com.alibaba.util.Utils.registerClassLoader(uccl, "uccl");
        uccl.loadClass(mainClass).getDeclaredMethod("main", String[].class).invoke(null, new Object[]{args});
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        String path = toPath(name);
        int index = find(path);
        if (index == -1) {
            return super.loadClass(name);
        } else {
            JarEntry entry = jarFiles[index].getJarEntry(path);
            try (InputStream is = jarFiles[index].getInputStream(entry)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream((int) entry.getSize());
                byte[] buffer = new byte[4096];
                int n;
                for (; (n = is.read(buffer)) != -1; ) {
                    baos.write(buffer, 0, n);
                }

                CodeSigner[] signers = null;
                CodeSource codeSource = new CodeSource(jarFileUrls[index], signers);
                return defineClass(name, baos.toByteArray(), 0, baos.size(), codeSource);
            } catch (IOException e) {
                throw new ClassNotFoundException(e.getMessage());
            }
        }
    }

    @Override
    public URL findResource(String name) {
        int index = find(name);
        if (index != -1) {
            try {
                return new URL("jar:" + jarFileUrls[index] +"!/");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    private int find(String name) {
        for (int i = 0; i < classes.length; i++) {
            if (classes[i].contains(name)) {
                return i;
            }
        }
        return -1;
    }

    private String toPath(String name) {
        String path = name.replace('.', '/') + ".class";
        return path;
    }

    @Override
    public URL[] getURLs() {
        return jarFileUrls;
    }
}
