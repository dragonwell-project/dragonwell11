/*
 * Copyright (c) 2024, Alibaba Group Holding Limited. All rights reserved.
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
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracProcess;
import jdk.test.lib.crac.CracTest;

import javax.crac.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * @test
 * @requires (os.family == "linux")
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @library /test/lib
 * @build PseudoPersistentFileTest
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest
 */
public class PseudoPersistentFileTest implements CracTest {

    private Resource resource;

    @Override
    public void test() throws Exception {
        CracBuilder cracBuilder = new CracBuilder().captureOutput(true)
                                          .restorePipeStdOutErr(true);
        CracProcess crProcess = cracBuilder.startCheckpoint();
        crProcess.waitForCheckpointed();
        cracBuilder.doRestore();
    }

    @Override
    public void exec() throws Exception {
        File f1 = new File("f1.txt");
        BufferedWriter bw1 = new BufferedWriter(new FileWriter(f1));
        bw1.write("file f1");

        File f2 = new File("f2.txt");
        BufferedWriter bw2 = new BufferedWriter(new FileWriter(f2));
        bw2.write("file f2");

        resource = new Resource() {
            @Override
            public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
                Core.registerPseudoPersistent(f1.getAbsolutePath(), Core.SAVE_RESTORE | Core.COPY_WHEN_RESTORE);
                Core.registerPseudoPersistent(f2.getAbsolutePath(), Core.SAVE_RESTORE | Core.SYMLINK_WHEN_RESTORE);
            }

            @Override
            public void afterRestore(Context<? extends Resource> context) throws Exception {
            }
        };
        Core.getGlobalContext().register(resource);
        Core.checkpointRestore();
        bw1.close();
        bw2.close();
    }
}
