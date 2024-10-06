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
import static jdk.test.lib.Asserts.*;

import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracProcess;
import jdk.test.lib.crac.CracTest;
import jdk.test.lib.crac.CracTestArg;
import jdk.test.lib.util.FileUtils;

import javax.crac.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @test
 * @library /test/lib
 * @build AppendOnlyFileTest
 * @summary Test C&R when open file with write&append mode that no need closed.
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest ALL KEEP_ALL
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest BY_EXT REMOVE_ALL
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest BY_FULL_PATH REMOVE_ONE_FILE
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest EXT_AND_FULL_PATH REMOVE_ONE_DIR
 *
 */
public class AppendOnlyFileTest implements CracTest {
    private static Path LOG_BASE;
    private static List<Path> LOG_FILES = new ArrayList<>();
    private static String TEXT_BEFORE_CHECKPOINT = "text before checkpoint";
    private static String TEXT_AFTER_RESTORE = "text after restore";
    private static int DELETE_FILE_INDEX = 0;
    private static int DELETE_DIR_INDEX = 2;

    static {
        LOG_BASE = Path.of(System.getProperty("user.dir"), "testlogs");
        LOG_FILES.add(LOG_BASE.resolve("a.log"));
        LOG_FILES.add(LOG_BASE.resolve("log1").resolve("b.msg"));
        LOG_FILES.add(LOG_BASE.resolve("log2").resolve("c.txt"));
    }

    private enum IgnoreAppendFileParamType {
        ALL,
        BY_EXT,
        BY_FULL_PATH,
        EXT_AND_FULL_PATH;
    }

    private enum KeepFilesPolicy {
        KEEP_ALL,
        REMOVE_ALL,
        REMOVE_ONE_FILE,
        REMOVE_ONE_DIR
    }

    private Resource resource;

    @CracTestArg(0)
    IgnoreAppendFileParamType paramType;

    @CracTestArg(1)
    KeepFilesPolicy keepFilesPolicy;

    @Override
    public void test() throws Exception {
        resetLogDir();

        CracBuilder cracBuilder = new CracBuilder().printResources(true)
                                .appendOnlyFiles(getAppendOnlyParam(paramType))
                                .captureOutput(true);
        CracProcess crProcess = cracBuilder.startCheckpoint();
        int ret = crProcess.waitFor();
        if (ret != 137) {
            crProcess.outputAnalyzer().reportDiagnosticSummary();
            assertEquals(137, ret, "Checkpointed process was not killed as expected.");
        }

        keepFilesByPolicy(keepFilesPolicy);
        cracBuilder.doRestore();
        validateFile(keepFilesPolicy);
    }

    private static void resetLogDir() throws IOException {
        if (LOG_BASE.toFile().exists()) {
            FileUtils.deleteFileTreeWithRetry(LOG_BASE);
        }
        for (Path logFile : LOG_FILES) {
            logFile.getParent().toFile().mkdirs();
        }
    }

    @Override
    public void exec() throws Exception {
        List<PrintWriter> writers = LOG_FILES.stream().map((f) -> {
            PrintWriter pw = openWithWriteAppendMode(f);
            pw.println(TEXT_BEFORE_CHECKPOINT);
            pw.flush();
            return pw;
        }).collect(Collectors.toList());

        Core.checkpointRestore();

        writers.stream().forEach((pw) -> {
            pw.println(TEXT_AFTER_RESTORE);
            pw.flush();
            pw.close();
        });
    }

    private PrintWriter openWithWriteAppendMode(Path filePath) {
        try {
            return new PrintWriter(new BufferedWriter(new FileWriter(filePath.toFile(), true)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateFile(KeepFilesPolicy keepFilesPolicy) throws IOException {
        for (int i = 0; i < LOG_FILES.size(); i++) {
            Path logFile = LOG_FILES.get(i);
            assertTrue(logFile.toFile().exists());
            List<String> lines = Files.lines(logFile).collect(Collectors.toList());

            assertTrue(lines.stream().anyMatch((t) -> t.equals(TEXT_AFTER_RESTORE)));

            if (keepFilesPolicy == KeepFilesPolicy.KEEP_ALL) {
                assertContainBeforeCheckpoint(true, lines);
            } else if (keepFilesPolicy == KeepFilesPolicy.REMOVE_ONE_FILE) {
                assertContainBeforeCheckpoint(i != DELETE_FILE_INDEX, lines);
            } else if (keepFilesPolicy == KeepFilesPolicy.REMOVE_ALL) {
                assertContainBeforeCheckpoint(false, lines);
            } else if (keepFilesPolicy == KeepFilesPolicy.REMOVE_ONE_DIR) {
                assertContainBeforeCheckpoint(i != DELETE_DIR_INDEX, lines);
            }
        }
    }

    private void assertContainBeforeCheckpoint(boolean contain, List<String> lines) {
        if (contain) {
            assertTrue(lines.stream().anyMatch((t) -> t.equals(TEXT_BEFORE_CHECKPOINT)));
        } else {
            assertFalse(lines.stream().anyMatch((t) -> t.equals(TEXT_BEFORE_CHECKPOINT)));
        }
    }

    private void keepFilesByPolicy(KeepFilesPolicy keepFilesPolicy) throws IOException {
        if (keepFilesPolicy == KeepFilesPolicy.REMOVE_ALL) {
            FileUtils.deleteFileTreeWithRetry(LOG_BASE);
        } else if (keepFilesPolicy == KeepFilesPolicy.REMOVE_ONE_FILE) {
            assertTrue(LOG_FILES.get(DELETE_FILE_INDEX).toFile().delete());
        } else if (keepFilesPolicy == KeepFilesPolicy.REMOVE_ONE_DIR) {
            FileUtils.deleteFileTreeWithRetry(LOG_FILES.get(DELETE_DIR_INDEX));
        }
    }

    private String getAppendOnlyParam(IgnoreAppendFileParamType paramType) {
        switch (paramType) {
            case ALL:
                return "*";
            case BY_EXT:
                return "*.log,*.txt,*.msg";
            case BY_FULL_PATH:
                return LOG_FILES.stream().map((p) -> getCanonicalPath(p)).collect(Collectors.joining(","));
            case EXT_AND_FULL_PATH:
                return "*.log,*.msg," + getCanonicalPath(LOG_FILES.get(LOG_FILES.size() - 1));
            default:
                throw new RuntimeException("unknown IgnoreAppendFileParamType type : " + paramType);
        }
    }

    private String getCanonicalPath(Path path) {
        try {
            return path.toFile().getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
