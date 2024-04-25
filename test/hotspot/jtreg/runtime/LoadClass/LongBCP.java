/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary JVM should be able to handle full path (directory path plus
 *          class name) or directory path longer than MAX_PATH specified
 *          in -Xbootclasspath/a on windows.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @run main LongBCP
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class LongBCP {

    private static final int MAX_PATH = 260;

    public static void main(String args[]) throws Exception {
        Path sourceDir = Paths.get(System.getProperty("test.src"), "test-classes");
        Path classDir = Paths.get(System.getProperty("test.classes"));
        Path destDir = classDir;

        // create a sub-path so that the destDir length is almost MAX_PATH
        // so that the full path (with the class name) will exceed MAX_PATH
        int subDirLen = MAX_PATH - classDir.toString().length() - 2;
        if (subDirLen > 0) {
            char[] chars = new char[subDirLen];
            Arrays.fill(chars, 'x');
            String subPath = new String(chars);
            destDir = Paths.get(System.getProperty("test.classes"), subPath);
        }

        CompilerUtils.compile(sourceDir, destDir);

        String bootCP = "-Xbootclasspath/a:" + destDir.toString();
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // increase the length of destDir to slightly over MAX_PATH
        destDir = Paths.get(destDir.toString(), "xxxxx");
        CompilerUtils.compile(sourceDir, destDir);

        bootCP = "-Xbootclasspath/a:" + destDir.toString();
        pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // create a hello.jar
        sun.tools.jar.Main jarTool = new sun.tools.jar.Main(System.out, System.err, "jar");
        String helloJar = destDir.toString() + File.separator + "hello.jar";
        if (!jarTool.run(new String[]
            {"-cf", helloJar, "-C", destDir.toString(), "Hello.class"})) {
            throw new RuntimeException("Could not write the Hello jar file");
        }

        // run with long bootclasspath to hello.jar
        bootCP = "-Xbootclasspath/a:" + helloJar;
        pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // relative path tests
        // We currently cannot handle relative path specified in the
        // -Xbootclasspath/a on windows.
        //
        // relative path length within the file system limit
        int fn_max_length = 255;
        // In AUFS file system, the maximal file name length is 242
        FileStore store = Files.getFileStore(new File(".").toPath());
        String fs_type = store.type();
        if ("aufs".equals(fs_type)) {
            fn_max_length = 242;
        }
        char[] chars = new char[fn_max_length];
        Arrays.fill(chars, 'y');
        String subPath = new String(chars);
        destDir = Paths.get(".", subPath);

        CompilerUtils.compile(sourceDir, destDir);

        bootCP = "-Xbootclasspath/a:" + destDir.toString();
        pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // total relative path length exceeds MAX_PATH
        destDir = Paths.get(destDir.toString(), "yyyyyyyy");

        CompilerUtils.compile(sourceDir, destDir);

        bootCP = "-Xbootclasspath/a:" + destDir.toString();
        pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);
    }
}
