// Copyright 2019-2021 Azul Systems, Inc.  All Rights Reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License version 2 only, as published by
// the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE.  See the GNU General Public License version 2 for more
// details (a copy is included in the LICENSE file that accompanied this code).
//
// You should have received a copy of the GNU General Public License version 2
// along with this work; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Azul Systems, 385 Moffett Park Drive, Suite 115, Sunnyvale,
// CA 94089 USA or visit www.azul.com if you need additional information or
// have any questions.

import jdk.crac.*;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracTest;
import jdk.test.lib.crac.CracLogger;
import jdk.test.lib.crac.CracTestArg;

import java.security.SecureRandom;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertNotEquals;

/*
 * @test
 * @summary Verify that SHA1PRNG secure random is reseeded after restore if initialized with default seed.
 * @library /test/lib
 * @build ReseedTest
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest true
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest false
 */
public class ReseedTest extends CracLogger implements CracTest {
    @CracTestArg
    boolean reseed;

    @Override
    public void test() throws Exception {
        CracBuilder builder = new CracBuilder();
        builder.doCheckpoint();
        builder.logToFile(true);
        String e1 = builder.doRestore().fileOutputAnalyser().getStdout();
        String e2 = builder.doRestore().fileOutputAnalyser().getStdout();
        if (reseed) {
            assertEquals(e1, e2);
        } else {
            assertNotEquals(e1, e2);
        }
    }

    @Override
    public void exec() throws Exception {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        if (reseed) {
            sr.setSeed(sr.generateSeed(10));
        }
        sr.nextInt();

        try {
            jdk.crac.Core.checkpointRestore();
        } catch (CheckpointException e) {
            e.printStackTrace(System.out);
            throw new RuntimeException("Checkpoint ERROR " + e);
        } catch (RestoreException e) {
            throw new RuntimeException("Restore ERROR " + e);
        }

        writeLog(String.valueOf(sr.nextInt()));
    }
}
