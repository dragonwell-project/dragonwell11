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

import java.io.*;
import java.lang.ref.Cleaner;

import jdk.crac.*;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracEngine;
import jdk.test.lib.crac.CracTest;

/**
 * @test
 * @requires (os.family == "linux")
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @library /test/lib
 * @build RefQueueTest
 * @run driver jdk.test.lib.crac.CracTest
 */
public class RefQueueTest implements CracTest {
    private static final Cleaner cleaner = Cleaner.create();

    @Override
    public void test() throws Exception {
        new CracBuilder().engine(CracEngine.SIMULATE)
                .startCheckpoint().waitForSuccess();
    }

    @Override
    public void exec() throws Exception {
        File badFile = File.createTempFile("jtreg-RefQueueTest", null);
        OutputStream badStream = new FileOutputStream(badFile);
        badStream.write('j');
        badFile.delete();

        // the cleaner would be able to run right away
        cleaner.register(new Object(), () -> {
            try {
                badStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // should close the file and only then go to the native checkpoint
        Core.checkpointRestore();
    }
}
