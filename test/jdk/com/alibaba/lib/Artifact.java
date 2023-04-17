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
import java.util.*;
import java.util.stream.Collectors;

public class Artifact {
    private String id;
    private PackageType packageType;
    private String deployDir;
    private String finalName;
    private String[] dependency;
    private JavaSource[] javaSources;
    private Artifact[] innerArtifacts;
    private ArtifactOption[] options;
    private JarSigner jarSigner;

    private Artifact(String id, PackageType packageType, String deployDir, String finalName, String[] dependency, JavaSource[] javaSources, Artifact[] innerArtifacts, ArtifactOption[] options, JarSigner jarSigner) {
        this.id = id;
        this.packageType = packageType;
        this.deployDir = deployDir;
        this.finalName = finalName;
        this.dependency = dependency;
        this.javaSources = javaSources;
        this.innerArtifacts = innerArtifacts;
        this.options = options;
        this.jarSigner = jarSigner;
    }

    public static Artifact createPlainJar(String id, String deployDir, String finalName, String[] dependency, JavaSource[] javaSources) {
        return new Artifact(id, PackageType.PLAIN_JAR, deployDir, finalName, dependency, javaSources, null, null, null);
    }

    public static Artifact createFatJar(String id, String deployDir, String finalName, Artifact[] innerArtifacts) {
        return new Artifact(id, PackageType.FAT_JAR, deployDir, finalName, null, JavaSource.FATJAR_TRAMPOLINE_SOURCE, innerArtifacts, null, null);
    }

    public static Artifact createPlainJar(String id, String deployDir, String finalName, String[] dependency, JavaSource[] javaSources, ArtifactOption[] options) {
        return new Artifact(id, PackageType.PLAIN_JAR, deployDir, finalName, dependency, javaSources, null, options, null);
    }

    public static Artifact createSignPlainJar(String id, String deployDir, String finalName, String[] dependency, JavaSource[] javaSources, ArtifactOption[] options, JarSigner jarSigner) {
        return new Artifact(id, PackageType.PLAIN_JAR, deployDir, finalName, dependency, javaSources, null, options, jarSigner);
    }

    public static Artifact createFatJar(String id, String deployDir, String finalName, Artifact[] innerArtifacts, ArtifactOption[] options) {
        return new Artifact(id, PackageType.FAT_JAR, deployDir, finalName, null, JavaSource.FATJAR_TRAMPOLINE_SOURCE, innerArtifacts, options, null);
    }

    public static Artifact createClasses(String id, String deployDir, String[] dependency, JavaSource[] javaSources) {
        return new Artifact(id, PackageType.CLASSES, deployDir, null, dependency, javaSources, null, null, null);
    }

    public static Artifact createClasses(String id, String deployDir, String[] dependency, JavaSource[] javaSources,ArtifactOption[] options) {
        return new Artifact(id, PackageType.CLASSES, deployDir, null, dependency, javaSources, null, options, null);
    }
    public Artifact clone(JavaSource[] newSources) {
        return new Artifact(id, packageType, deployDir, finalName, dependency, newSources, innerArtifacts, options, jarSigner);
    }

    public Artifact clone(Artifact[] newInnerArtifacts) {
        return new Artifact(id, packageType, deployDir, finalName, dependency, javaSources, newInnerArtifacts, options, jarSigner);
    }

    public String getId() {
        return id;
    }

    public PackageType getPackageType() {
        return packageType;
    }

    public String getFinalName() {
        return finalName;
    }

    public String getDeployDir() {
        return deployDir;
    }

    public String[] getDependency() {
        return dependency;
    }

    public JavaSource[] getJavaSources() {
        return javaSources;
    }

    public Artifact[] getInnerArtifacts() {
        return innerArtifacts;
    }

    public ArtifactOption[] getOptions() {
        return options;
    }

    public void build(ProjectWorkDir workDir, Map<String, Artifact> artifactMap, RunConf runConf) throws IOException {
        switch (packageType) {
            case CLASSES:
                buildClasses(workDir, artifactMap);
                break;
            case PLAIN_JAR:
                buildPlainJar(workDir, artifactMap);
                break;
            case FAT_JAR:
                buildFatJar(workDir, artifactMap, runConf);
                break;
            default:
                throw new RuntimeException("unknown package type : " + packageType);
        }
    }

    private void buildPlainJar(ProjectWorkDir workDir, Map<String, Artifact> artifactMap) throws IOException {
        //1. compile classes to playground
        workDir.resetPlayground();
        CompileUtil.compile(javaSources, workDir.getPlayground(), getOptions(workDir.getBuild(), artifactMap));
        //2. create jar in playground
        String destJar = JarUtil.writePlainJar(new File(workDir.getBuild(), getFileRelativePath()).getCanonicalPath(),
                workDir.getPlayground(), options);
        if (jarSigner != null) {
            jarSigner.sign(destJar);
        }
    }

    private void buildFatJar(ProjectWorkDir workDir, Map<String, Artifact> artifactMap, RunConf runConf) throws IOException {
        //1. build inner first
        List<Artifact> artifacts = topoSort(innerArtifacts, artifactMap);
        for (Artifact inner : artifacts) {
            inner.build(workDir, artifactMap, runConf);
        }
        //2. build myself to playground
        workDir.resetPlayground();
        CompileUtil.compile(javaSources, workDir.getPlayground(), getOptions(workDir.getBuild(), artifactMap));
        //3. package all into a single fat jar.
        //3.1 add myself classes
        String destJar = JarUtil.writeFatJar(runConf.mainClass(),
                new File(workDir.getBuild(), getFileRelativePath()).getCanonicalPath(),
                workDir.getPlayground(),
                Arrays.stream(innerArtifacts).map((i) -> new File(workDir.getBuild(), i.getFileRelativePath())).collect(Collectors.toList()),
                options
        );
        if (jarSigner != null) {
            jarSigner.sign(destJar);
        }
    }

    private void buildClasses(ProjectWorkDir workDir, Map<String, Artifact> artifactMap) throws IOException {
        //for classes that no need additional package operation
        File dest = new File(workDir.getBuild(), deployDir);
        if (!dest.exists() && !dest.mkdirs()) {
            throw new RuntimeException("Cannot create directory :" + dest + " for compile.");
        }
        CompileUtil.compile(javaSources, dest, getOptions(workDir.getBuild(), artifactMap));
    }

    private List<String> getOptions(File buildDir, Map<String, Artifact> artifactMap) {
        if (null == getDependency()) {
            return null;
        } else {
            Set<String> paths = new HashSet<>();
            for (String d : getDependency()) {
                Artifact artifact = artifactMap.get(d);
                paths.add(new File(buildDir, artifact.getFileRelativePath()).getAbsolutePath());
                if (artifact.getDependency() != null) {
                    addPaths(paths, buildDir, artifact.getDependency(), artifactMap);
                }
            }
            if (paths.isEmpty()) {
                return null;
            } else {
                return Arrays.asList("-classpath", String.join(File.pathSeparator, paths));
            }
        }
    }

    private void addPaths(Set<String> paths, File buildDir, String[] dependency, Map<String, Artifact> artifactMap) {
        for (String d : dependency) {
            Artifact artifact = artifactMap.get(d);
            paths.add(new File(buildDir, artifact.getFileRelativePath()).getAbsolutePath());
            if (artifact.getDependency() != null) {
                addPaths(paths, buildDir, artifact.getDependency(), artifactMap);
            }
        }
    }

    public String getFileRelativePath() {
        if (PackageType.CLASSES == packageType) {
            return deployDir;
        } else {
            return deployDir + File.separator + finalName;
        }
    }

    public static List<Artifact> topoSort(Artifact[] artifacts, Map<String, Artifact> artifactMap) {
        for (Artifact artifact : artifacts) {
            artifactMap.put(artifact.id, artifact);
        }
        List<String> prepared = new ArrayList<>(artifacts.length);
        List<Artifact> list = new ArrayList<>(Arrays.asList(artifacts));
        //find root node that not depend on anyone
        Iterator<Artifact> iter = list.iterator();
        while (iter.hasNext()) {
            Artifact artifact = iter.next();
            if (artifact.getDependency() == null || artifact.getDependency().length == 0) {
                prepared.add(artifact.getId());
                iter.remove();
            }
        }

        if (prepared.isEmpty()) {
            throw new RuntimeException("Not found the root artifact which not depend on other artifact!");
        }

        boolean changed = false;
        do {
            iter = list.iterator();
            while (iter.hasNext()) {
                Artifact artifact = iter.next();
                if (inList(artifact.getDependency(), prepared)) {
                    prepared.add(artifact.getId());
                    iter.remove();
                    changed = true;
                }
            }
            if (list.isEmpty()) {
                break;
            }
        } while (changed);
        if (!list.isEmpty()) {
            throw new RuntimeException(list.get(0).getId() + " not find dependent artifact!");
        }

        return prepared.stream().map((id) -> artifactMap.get(id)).collect(Collectors.toList());
    }

    private static boolean inList(String[] str, List<String> list) {
        for (String s : str) {
            if (list.contains(s)) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }
}
