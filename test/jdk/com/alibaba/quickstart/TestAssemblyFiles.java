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

/*
 * @test
 * @summary Test files assemblied into JDK
 * @library /test/lib
 * @library /lib/testlibrary
 * @requires os.arch=="amd64" | os.arch=="aarch64"
 * @run main/othervm TestAssemblyFiles
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static jdk.testlibrary.Asserts.assertTrue;

public class TestAssemblyFiles {

    private static final String SERVERLESS_ADAPTER    = "serverless/serverless-adapter.jar";
    private static final String QUICKSTART_CONFIG     = "quickstart.properties";

    public static void main(String[] args) throws Exception {
        verifyAssemblyFiles();
    }

    private static Path getJDKHome() {
        String jdkHome = System.getProperty("java.home");
        if (!new File(jdkHome).exists()) {
            throw new Error("Fatal error, cannot find jdk path: [" + jdkHome + "] doesn't exist!");
        }
        return Path.of(jdkHome);
    }

    private static Path getJDKBinDir() {
        return getJDKHome().resolve("bin");
    }

    private static Path getJDKLibDir() {
        return getJDKHome().resolve("lib");
    }

    private static Path getJDKConfDir() {
        return getJDKHome().resolve("conf");
    }

    private static void verifyAssemblyFiles() throws Exception {
        verifyQuickStartProperties();
        verifyServerlessAdapter();
    }

    private static void verifyQuickStartProperties() {
        assertTrue(getJDKConfDir().resolve(QUICKSTART_CONFIG).toFile().exists(), "must have " + QUICKSTART_CONFIG);
    }

    private static void verifyServerlessAdapter() {
        assertTrue(getJDKLibDir().resolve(SERVERLESS_ADAPTER).toFile().exists(), "must have " + SERVERLESS_ADAPTER);
    }
}
