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
import jdk.test.lib.crac.CracTestArg;

import java.io.IOException;
import java.security.SecureRandom;

/*
 * @test
 * @summary Verify that secure random is not interlocked during checkpoint/restore.
 * @library /test/lib
 * @build InterlockTest
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest SHA1PRNG 50
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest NativePRNGBlocking 50
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest NativePRNGNonBlocking 50
 * @run driver/timeout=60 jdk.test.lib.crac.CracTest NativePRNG 50
 */
public class InterlockTest implements Resource, CracTest {
    private static final long MIN_TIMEOUT = 100;
    private static final long MAX_TIMEOUT = 1000;

    private boolean stop = false;
    private SecureRandom sr;

    @CracTestArg(0)
    String algName;

    @CracTestArg(1)
    int numThreads;

    private class TestThread1 extends Thread {
        @Override
        public void run() {
            while (!stop) {
                set();
            }
        }
    };

    private class TestThread2 extends Thread implements Resource {
        private final SecureRandom sr;

        synchronized void set() {
            sr.nextInt();
        }
        synchronized void clean() {
            sr.nextInt();
        }

        TestThread2() throws Exception {
            sr = SecureRandom.getInstance(algName);
            Core.getGlobalContext().register(this);
        }

        @Override
        public void run() {
            while (!stop) {
                set();
            }
        }

        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            clean();
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws Exception {
            set();
        }
    };

    synchronized void clean() {
        sr.nextInt();
    }

    synchronized void set() {
        sr.nextInt();
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        try {
            clean();
        } catch(Exception e) {
            e.printStackTrace(System.out);
        };
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        set();
        stop = true;
    }

    @Override
    public void test() throws Exception {
        new CracBuilder().doCheckpointAndRestore();
    }

    @Override
    public void exec() throws Exception {
        sr = SecureRandom.getInstance(algName);
        Core.getGlobalContext().register(this);

        Thread[] threads = new Thread[numThreads];
        for(int i = 0; i < numThreads; i++) {
            threads[i] = (i % 2 == 0) ?
                    new TestThread1():
                    new TestThread2();
            threads[i].start();
        };
        Thread.sleep(MIN_TIMEOUT);
        set();
        Thread.sleep(MIN_TIMEOUT);

        Object checkpointLock = new Object();
        Thread checkpointThread = new Thread("checkpointThread") {
            public void run() {
                synchronized (checkpointLock) {
                    try {
                        jdk.crac.Core.checkpointRestore();
                    } catch (CheckpointException e) {
                        throw new RuntimeException("Checkpoint ERROR " + e);
                    } catch (RestoreException e) {
                        throw new RuntimeException("Restore ERROR " + e);
                    }
                    checkpointLock.notify();
                }
            }
        };
        synchronized (checkpointLock) {
            try {
                checkpointThread.start();
                checkpointLock.wait(MAX_TIMEOUT * 2);
            } catch(Exception e){
                throw new RuntimeException("Checkpoint/Restore ERROR " + e);
            }
        }
        Thread.sleep(MAX_TIMEOUT);
    }
}
