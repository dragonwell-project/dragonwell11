/*
 * Copyright (c) 2022, Azul Systems, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import jdk.test.lib.crac.CracLogger;
import jdk.test.lib.crac.CracTest;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @requires (os.family == "linux")
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @library /test/lib
 * @build LeaveRunning
 * @run driver jdk.test.lib.crac.CracTest
 */
public class LeaveRunning extends CracLogger implements CracTest {
    @Override
    public void test() throws Exception {
        CracBuilder builder = new CracBuilder().env("CRAC_CRIU_LEAVE_RUNNING", "")
                .logToFile(true);
        builder.startCheckpoint().waitForSuccess().fileOutputAnalyser().shouldContain(RESTORED_MESSAGE);
        builder.doRestore().fileOutputAnalyser().shouldContain(RESTORED_MESSAGE);
    }

    @Override
    public void exec() throws Exception {
        Core.checkpointRestore();
        writeLog(RESTORED_MESSAGE);
    }
}
