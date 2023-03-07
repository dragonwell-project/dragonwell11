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
import java.util.List;

public class RunJarConf implements RunConf {

    private final String mainClass;
    private final String mainJarArtifactId;

    public RunJarConf(String mainClass, String mainJarArtifactId) {
        this.mainClass = mainClass;
        this.mainJarArtifactId = mainJarArtifactId;
    }

    @Override
    public String[] buildJavaRunCommands(File buildDir, Artifact[] artifacts) {
        List<String> commands = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            if (artifact.getId().equals(mainJarArtifactId)) {
                commands.add("-jar");
                commands.add(new File(buildDir, artifact.getFileRelativePath()).getAbsolutePath());
            }
        }
        return commands.toArray(new String[commands.size()]);
    }

    @Override
    public String mainClass() {
        return mainClass;
    }
}
