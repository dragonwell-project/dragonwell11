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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunWithURLClassLoaderConf implements RunConf {
    private final String mainClass;

    public RunWithURLClassLoaderConf(String mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public String[] buildJavaRunCommands(File buildDir, Artifact[] artifacts) {
        List<String> commands = new ArrayList<>();
        commands.add("URLClassLoaderLauncher");
        commands.add(mainClass);
        if (artifacts != null && artifacts.length > 0) {
            for (Artifact artifact : artifacts) {
                if (loadWithURLClassLoader(artifact)) {
                    commands.add(new File(buildDir, artifact.getFileRelativePath()).getAbsolutePath());
                }
            }
        }
        return commands.toArray(new String[commands.size()]);
    }

    @Override
    public List<String> classpath(File buildDir, Artifact[] artifacts) {
        //library that load by URLClassLoader,so no need add to class path.
        if (artifacts != null && artifacts.length > 0) {
            List<String> paths = new ArrayList<>();
            for (Artifact artifact : artifacts) {
                if (!loadWithURLClassLoader(artifact)) {
                    paths.add(new File(buildDir, artifact.getFileRelativePath()).getAbsolutePath());
                }
            }
            return paths;
        } else {
            return null;
        }
    }

    private boolean loadWithURLClassLoader(Artifact artifact) {
        return artifact.getOptions() != null && Arrays.asList(artifact.getOptions()).contains(ArtifactOption.LOAD_BY_URLCLASSLOADER);
    }

    @Override
    public String mainClass() {
        return mainClass;
    }
}
