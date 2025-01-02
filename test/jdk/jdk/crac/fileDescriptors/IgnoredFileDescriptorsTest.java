/*
 * Copyright (c) 2023, Azul Systems, Inc. All rights reserved.
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

import jdk.crac.Core;
import jdk.test.lib.Utils;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracLogger;
import jdk.test.lib.crac.CracTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @test
 * @requires os.family == "linux"
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @library /test/lib
 * @build IgnoredFileDescriptorsTest
 * @run driver jdk.test.lib.crac.CracTest
 */
public class IgnoredFileDescriptorsTest extends CracLogger implements CracTest {
    private static final String EXTRA_FD_WRAPPER = Path.of(Utils.TEST_SRC, "extra_fd_wrapper.sh").toString();

    @Override
    public void test() throws Exception {
        List<String> prefix = new ArrayList<>();
        prefix.add(EXTRA_FD_WRAPPER);
        prefix.addAll(Arrays.asList("-o", "43", "/dev/stdout"));
        prefix.addAll(Arrays.asList("-o", "45", "/dev/urandom"));
        prefix.add(CracBuilder.JAVA);
        prefix.add("-XX:CRaCIgnoredFileDescriptors=43,/dev/null,44,/dev/urandom");

        CracBuilder builder = new CracBuilder();
        builder.startCheckpoint(prefix).waitForCheckpointed();
        builder.logToFile(true).doRestore().fileOutputAnalyser().shouldContain(RESTORED_MESSAGE);
    }

    @Override
    public void exec() throws Exception {
        try (var stream = Files.list(Path.of("/proc/self/fd"))) {
            Map<Integer, String> fds = stream.filter(Files::isSymbolicLink)
                    .collect(Collectors.toMap(
                            f -> Integer.parseInt(f.toFile().getName()),
                            f -> {
                                try {
                                    return Files.readSymbolicLink(f).toFile().getAbsoluteFile().toString();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }));
            if (fds.containsKey(42)) {
                throw new IllegalStateException("Oh no, 42 was not supposed to be ignored");
            } else if (!fds.containsKey(0) || !fds.containsKey(1) || !fds.containsKey(2)) {
                throw new IllegalStateException("Missing standard I/O? Available: " + fds);
            } else if (!fds.containsKey(43)) {
                throw new IllegalStateException("Missing FD 43");
            } else if (!fds.containsValue("/dev/urandom")) {
                throw new IllegalStateException("Missing /dev/urandom");
            }
        }
        Core.checkpointRestore();
        writeLog(RESTORED_MESSAGE);
    }
}
