/*
 * Copyright (c) 2022-2023, Azul Systems, Inc. All rights reserved.
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

import jdk.crac.*;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracEngine;
import jdk.test.lib.crac.CracTest;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @test JarFileFactoryCacheTest
 * @library /test/lib
 * @build JarFileFactoryCacheTest
 * @run driver jdk.test.lib.crac.CracTest
 */
public class JarFileFactoryCacheTest implements CracTest {
    @Override
    public void test() throws Exception {
        new CracBuilder().engine(CracEngine.SIMULATE).printResources(true)
                .startCheckpoint().waitForSuccess();
    }

    @Override
    public void exec() throws Exception {
        Path temp = Files.createTempDirectory(JarFileFactoryCacheTest.class.getName());
        Path testFilePath = temp.resolve("test.txt");
        try {
            Files.writeString(testFilePath, "test\n");
            jdk.test.lib.util.JarUtils.createJarFile(
                    Path.of("test.jar"), temp, "test.txt");
        } finally {
            File testTxt = testFilePath.toFile();
            if (testTxt.exists()) {
                assert testTxt.delete();
            }
            assert temp.toFile().delete();
        }

        URL url = new URL("jar:file:test.jar!/test.txt");
        InputStream inputStream = url.openStream();
        byte[] content = inputStream.readAllBytes();
        if (content.length != 5) {
            throw new AssertionError("wrong content: " + new String(content));
        }
        inputStream.close();
        // Nulling the variables is actually necessary!
        inputStream = null;
        url = null;

        Core.checkpointRestore();
    }
}
