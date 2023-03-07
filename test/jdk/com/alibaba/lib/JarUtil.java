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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class JarUtil {
    private static final String FARJAR_LIB_PATH = "BOOT-INF/lib/";
    private static final String FATJAR_CLASSES_PATH = "BOOT-INF/classes/";

    public static String writePlainJar(String destJarFile, File classes, ArtifactOption[] options) throws IOException {
        JarCreator jarCreator;
        if (options != null && Arrays.asList(options).contains(ArtifactOption.NO_MANIFEST)) {
            jarCreator = new JarCreator(destJarFile);
        } else {
            Map<Attributes.Name, String> manifestMap = new HashMap<>();
            manifestMap.put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
            jarCreator = new JarCreator(destJarFile, manifestMap);
        }
        List<Path> classFiles = Files.walk(classes.toPath())
                .filter(Files::isRegularFile).collect(Collectors.toList());
        for (Path classFile : classFiles) {
            jarCreator.addFile(classes.getCanonicalPath(), classFile.toFile().getCanonicalPath());
        }
        jarCreator.close();
        return destJarFile;
    }

    public static String writeFatJar(String mainClass, String destJarFile, File outerClasses, List<File> innerFiles, ArtifactOption[] options) throws IOException {
        JarCreator jarCreator;
        if (options != null && Arrays.asList(options).contains(ArtifactOption.NO_MANIFEST)) {
            jarCreator = new JarCreator(destJarFile);
        } else {
            Map<Attributes.Name, String> manifestMap = new HashMap<>();
            manifestMap.put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
            manifestMap.put(Attributes.Name.MAIN_CLASS, JavaSource.FATJAR_TRAMPOLINE_CLASSNAME);
            manifestMap.put(new Attributes.Name("Start-Class"), mainClass);
            jarCreator = new JarCreator(destJarFile, manifestMap);
        }
        jarCreator.addDirectoryEntry(FARJAR_LIB_PATH);
        jarCreator.addDirectoryEntry(FATJAR_CLASSES_PATH);
        List<Path> classFiles = Files.walk(outerClasses.toPath())
                .filter(Files::isRegularFile).collect(Collectors.toList());
        for (Path classFile : classFiles) {
            jarCreator.addFile(outerClasses.getCanonicalPath(), classFile.toFile().getCanonicalPath());
        }

        //3.2 add inner artifacts
        for (File inner : innerFiles) {
            if (inner.isDirectory()) {
                classFiles = Files.walk(inner.toPath()).filter(Files::isRegularFile).collect(Collectors.toList());
                for (Path classFile : classFiles) {
                    jarCreator.addFile(() -> FATJAR_CLASSES_PATH + getRelativePath(inner.getAbsolutePath(), classFile.toFile().getAbsolutePath()),
                            classFile.toFile().getAbsolutePath());
                }

            } else if (inner.getName().endsWith(".jar")) {
                jarCreator.addFile(() -> FARJAR_LIB_PATH + inner.getName(), inner.getAbsolutePath());
            } else {
                throw new RuntimeException("Fat jar only support contain classes and plain jar.but now is " + inner.getAbsolutePath());
            }
        }
        jarCreator.close();
        return destJarFile;
    }

    public static String readFarJar(FatJar fatJar, List<URL> innerLibClasses) throws IOException {
        JarFile jarFile = new JarFile(fatJar.getRoot());
        String startClass = jarFile.getManifest().getMainAttributes().getValue("Start-Class");
        boolean containClasses = false;
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().startsWith(FARJAR_LIB_PATH)) {
                String file = fatJar.getRoot().toURI() + "!/" + jarEntry.getName() + "!/";
                file = file.replace("file:////", "file://");
                innerLibClasses.add(new URL("jar", "", -1, file, new FatJarHandler(jarFile, fatJar)));
            } else if (jarEntry.getName().startsWith(FATJAR_CLASSES_PATH)) {
                containClasses = true;
            }
        }

        if (containClasses) {
            String file = fatJar.getRoot().toURI() + "!/" +
                    (FATJAR_CLASSES_PATH.endsWith("/") ? FATJAR_CLASSES_PATH.substring(0, FATJAR_CLASSES_PATH.length() - 1) : FATJAR_CLASSES_PATH)
                    + "!/";
            file = file.replace("file:////", "file://");
            innerLibClasses.add(new URL("jar", "", -1, file, new FatJarHandler(jarFile, fatJar)));
        }

        return startClass;
    }

    private static String getRelativePath(String rootPath, String fullPath) {
        if (rootPath.endsWith(File.separator)) {
            return fullPath.substring(rootPath.length());
        } else {
            return fullPath.substring(rootPath.length() + 1);
        }
    }

}
