/*
 * Copyright (c) 2022, Azul Systems, Inc. All rights reserved.
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
import java.io.FileOutputStream;
import java.io.IOException;

import jdk.crac.*;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracEngine;
import jdk.test.lib.crac.CracTest;

/**
 * @test DryRunTest
 * @requires (os.family == "linux")
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @library /test/lib
 * @build DryRunTest
 * @run driver jdk.test.lib.crac.CracTest
 */
public class DryRunTest implements CracTest {
    static class CRResource implements Resource {
        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            throw new RuntimeException("should not pass");
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws Exception {
        }
    }

    @Override
    public void test() throws Exception {
        new CracBuilder().engine(CracEngine.SIMULATE).printResources(true)
                .startCheckpoint().waitForSuccess();
    }

    @Override
    public void exec() throws Exception {
        Resource resource = new CRResource();
        Core.getGlobalContext().register(resource);

        File tempFile = File.createTempFile("jtreg-DryRunTest", null);
        FileOutputStream stream = new FileOutputStream(tempFile);
        stream.write('j');

        int exceptions = 0;

        try {
            Core.checkpointRestore();
        } catch (CheckpointException ce) {

            ce.printStackTrace();

            for (Throwable e : ce.getSuppressed()) {
                String name = e.getClass().getName();
                switch (name) {
                    case "java.lang.RuntimeException":                exceptions |= 0x1; break;
                    case "jdk.crac.impl.CheckpointOpenFileException": exceptions |= 0x2; break;
                }
            }
        }

        stream.close();
        tempFile.delete();

        if (exceptions != 0x3) {
            throw new RuntimeException("fail " + exceptions);
        }
    }
}
