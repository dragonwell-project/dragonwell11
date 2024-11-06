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
import jdk.test.lib.crac.*;

import java.util.concurrent.atomic.AtomicInteger;

/*
 * @test
 * @requires (os.family == "linux")
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @summary check that the recursive checkpoint is not allowed
 * @library /test/lib
 * @build Test
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest 10
 */
public class Test implements Resource, CracTest {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static Exception exception = null;

    @CracTestArg
    int numThreads;

    @Override
    public void test() throws Exception {
        CracBuilder builder = new CracBuilder().engine(CracEngine.PAUSE);
        CracProcess process = builder.startCheckpoint();
        //If image dir not exist, process.waitForPausePid() can throw an java.nio.file.NoSuchFileException.
        //This can happen when run this testcase on host with high load.
        process.ensureFileIntegrityForPausePid();
        process.waitForPausePid();
        for (int i = 1; i <= numThreads + 1; ++i) {
            System.err.printf("Restore #%d%n", i);
            process.ensureFileIntegrityForPausePid();
            builder.doRestore();
        }
        process.waitForSuccess();
    }

    private static class TestThread extends Thread {

        @Override
        public void run() {
            try {
                jdk.crac.Core.checkpointRestore();
            } catch (CheckpointException e) {
                if (exception == null)
                    exception = new RuntimeException("Checkpoint in thread ERROR " + e);
            } catch (RestoreException e) {
                if (exception == null)
                    exception = new RuntimeException("Restore in thread ERROR " + e);
            }
        }
    };

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        try {
            int c = counter.incrementAndGet();
            if (c > 1) {
                if (exception == null)
                    exception = new RuntimeException("Parallel checkpoint");
            }
            Thread.sleep(100);
            jdk.crac.Core.checkpointRestore();
            if (exception != null)
                exception = new RuntimeException("Checkpoint Exception should be thrown");
        } catch (CheckpointException e) {
            // Expected Exception
        } catch (RestoreException e) {
            if (exception == null)
                exception = new RuntimeException("Restore ERROR " + e);
        }
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        try {
            int c = counter.get();
            if (c > 1) {
                if (exception == null)
                    exception = new RuntimeException("Parallel checkpoint");
            }
            Thread.sleep(100);
            jdk.crac.Core.checkpointRestore();
            if (exception == null)
                exception = new RuntimeException("Checkpoint Exception should be thrown");
        } catch (CheckpointException e) {
            // Expected Exception
        } catch (RestoreException e) {
            if (exception == null)
                exception = new RuntimeException("Restore ERROR " + e);
        } finally {
            counter.decrementAndGet();
        }
    }

    @Override
    public void exec() throws Exception {
        Core.getGlobalContext().register(new Test());

        TestThread[] threads = new TestThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new TestThread();
            threads[i].start();
        };

        Thread.sleep(100);
        try {
            jdk.crac.Core.checkpointRestore();
        } catch (CheckpointException e) {
            throw new RuntimeException("Checkpoint ERROR " + e);
        } catch (RestoreException e) {
            throw new RuntimeException("Restore ERROR " + e);
        }

        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        };

        long ccounter = counter.get();
        if (ccounter != 0)
            throw new RuntimeException("Incorrect counter after restore: " + ccounter + " instead of 0");
        if (exception != null) {
            throw exception;
        }
        System.out.println("PASSED");
    }
}
