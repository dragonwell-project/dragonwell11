// Copyright 2019-2020 Azul Systems, Inc.  All Rights Reserved.
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

import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracTest;
import jdk.test.lib.crac.CracTestArg;

import java.nio.channels.Selector;
import java.io.IOException;

import java.util.concurrent.atomic.AtomicInteger;

/*
 * @test Selector/multipleSelectNow
 * @requires (os.family == "linux")
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @summary check work of multiple selectNow() + C/R peaceful coexistence
 * @library /test/lib
 * @build Test
 * @run driver jdk.test.lib.crac.CracTest false
 * @run driver jdk.test.lib.crac.CracTest true
 */
public class Test implements CracTest {

    @CracTestArg
    boolean skipCR;

    @Override
    public void test() throws Exception {
        CracBuilder builder = new CracBuilder();
        if (skipCR) {
            builder.doPlain();
        } else {
            builder.doCheckpointAndRestore();
        }
    }

    @Override
    public void exec() throws Exception {
        AtomicInteger nSelected = new AtomicInteger(0);

        Selector selector = Selector.open();

        int nThreads = skipCR ? 30 : 150; // some selectNow() calls should occur at the same time with C/R
        Thread threads[] = new Thread[nThreads];

        Runnable rStart = new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < threads.length; ++i) {

                    threads[i] = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                System.out.println("selectNow");
                                nSelected.incrementAndGet();
                                selector.selectNow();
                                System.out.println("done");
                                nSelected.decrementAndGet();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                    threads[i].start();
                    try { Thread.sleep(5); } catch (InterruptedException ie) {}
                }
            }
        };
        Thread tStart = new Thread(rStart);
        tStart.start();
        Thread.sleep(500);

        if (!skipCR) {
            jdk.crac.Core.checkpointRestore();
        }

        tStart.join();

        do { Thread.sleep(2000); } while (nSelected.get() > 0);
        for (Thread t: threads) { t.join(); } // just in case...

        // === check that the selector works as expected ===
        if (!selector.isOpen()) { throw new RuntimeException("the selector must be open"); }

        selector.wakeup();
        selector.select();

        selector.selectNow();
        selector.select(200);

        selector.close();
    }
}
