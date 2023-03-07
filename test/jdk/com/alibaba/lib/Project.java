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

import java.io.IOException;
import java.util.*;

public class Project {
    private final Artifact[] artifacts;
    private final ExpectOutput expectOutput;
    private RunConf runConf;

    public Project(RunConf runConf, Artifact[] artifacts, ExpectOutput expectOutput) {
        this.runConf = runConf;
        this.artifacts = artifacts;
        this.expectOutput = expectOutput;
    }

    public void build(ProjectWorkDir workDir) throws IOException {
        workDir.resetBuildDir();
        Map<String, Artifact> artifactMap = new HashMap<>();
        List<Artifact> sorted = Artifact.topoSort(artifacts, artifactMap);
        for (Artifact artifact : sorted) {
            artifact.build(workDir, artifactMap, runConf);
        }
    }


    public Artifact[] getArtifacts() {
        return artifacts;
    }

    public ExpectOutput getExpectOutput() {
        return expectOutput;
    }

    public RunConf getRunConf() {
        return runConf;
    }
}
