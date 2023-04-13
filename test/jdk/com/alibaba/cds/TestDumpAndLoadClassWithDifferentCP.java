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
 * @summary Test dump and load the class with different class path
 * @library /lib/testlibrary /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @modules jdk.compiler
 * @modules java.base/jdk.internal.loader:+open
 * @build Classes4CDS
 * @build trivial.ThrowException
 * @build TestAppClassLoader
 * @requires os.arch=="amd64" | os.arch=="aarch64"
 * @run driver ClassFileInstaller -jar testSimple.jar trivial.ThrowException
 * @run driver ClassFileInstaller -jar test.jar TestAppClassLoader
 * @run main/othervm -XX:+UnlockExperimentalVMOptions TestDumpAndLoadClassWithDifferentCP
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class TestDumpAndLoadClassWithDifferentCP {

    private static final String TESTJAR = "./testSimple.jar:./test.jar";
    private static final String TESTNAME = "TestAppClassLoader";

    private static final String CLASSLIST_FILE = "./TestDumpAndLoadClassWithDifferentCP.classlist";
    private static final String ARCHIVE_FILE = "./TestDumpAndLoadClassWithDifferentCP.jsa";

    public static void main(String[] args) throws Exception {

        // dump loaded classes into a classlist file
        dumpLoadedClasses();

        // create an archive using the classlist
        dumpArchive();

        // change source path
        changeSourcePath();

        // start the java process with shared archive file
        startWithJsa();
    }

    static void dumpLoadedClasses() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(true,
            "-XX:DumpLoadedClassList=" + CLASSLIST_FILE,
            // trigger JVMCI runtime init so that JVMCI classes will be
            // included in the classlist
            "-XX:+AppCDSClassFingerprintCheck",
            "-Xlog:class+cds=trace",
            "-cp",
            TESTJAR,
            TESTNAME);

        OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "dump-loaded-classes")
            .shouldHaveExitValue(0);
    }

    static void dumpArchive() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(true,
            "-cp",
            TESTJAR,
            "-XX:SharedClassListFile=" + CLASSLIST_FILE,
            "-XX:SharedArchiveFile=" + ARCHIVE_FILE,
            "-Xshare:dump",
            "-XX:+AppCDSClassFingerprintCheck",
            "-Xlog:class+cds=trace",
            "-XX:MetaspaceSize=12M",
            "-XX:MaxMetaspaceSize=12M");

        OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "dump-archive");
        int exitValue = output.getExitValue();
        if (exitValue == 1) {
            output.shouldContain("Failed allocating metaspace object type");
        } else if (exitValue == 0) {
            output.shouldContain("Loading classes to share");
        } else {
            throw new RuntimeException("Unexpected exit value " + exitValue);
        }
    }

    static void changeSourcePath() throws Exception {
        String classPath = System.getProperty("test.classes");
        String newClassPath = "newpath";
        File tmpFile = new File(newClassPath);
        if (!tmpFile.exists()) {
            tmpFile.mkdirs();
        }
        File afile = new File("./testSimple.jar");
        afile.renameTo(new File(newClassPath + "/testSimple.jar"));
    }

    static void startWithJsa() throws Exception {
        String classPath = "newpath/testSimple.jar:./test.jar";
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(true,
            "-Xshare:on",
            "-XX:SharedArchiveFile=" + ARCHIVE_FILE,
            "-XX:+AppCDSClassFingerprintCheck",
            "-Xlog:class+cds=trace",
            "-cp",
            classPath,
            TESTNAME);

        OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "start-with-shared-archive")
                                .shouldHaveExitValue(0);
        output.shouldContain("[Load Class Detail Info] Successful loading of class");
    }
}
