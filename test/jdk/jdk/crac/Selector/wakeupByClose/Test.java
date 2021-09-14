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

/*
 * @test Selector/wakeupByClose
 * @summary check that the Selector's close() wakes it up after restore
 * @library /test/lib
 * @build Test
 * @run driver jdk.test.lib.crac.CracTest true  false
 * @run driver jdk.test.lib.crac.CracTest false false
 * @run driver jdk.test.lib.crac.CracTest true  true
 * @run driver jdk.test.lib.crac.CracTest false true
 */
public class Test implements CracTest {

    static boolean awakened, closed;

    @CracTestArg(0)
    boolean setTimeout;

    @CracTestArg(1)
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
        Selector selector = Selector.open();

        Thread tSelect = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        awakened = false;
                        if (setTimeout) { selector.select(3600_000); }
                        else { selector.select(); }
                        awakened = true;
                    } catch (IOException e) { throw new RuntimeException(e); }
                }
            });
        tSelect.start();

        Thread.sleep(3000);

        if (!skipCR) { jdk.crac.Core.checkpointRestore(); }

        // close() must wakeup the selector
        Thread tClose = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        closed = false;
                        selector.close();
                        closed = true;
                    } catch (IOException e) { throw new RuntimeException(e); }
                }
            });
        tClose.start();
        tClose.join();
        tSelect.join();

        if (!awakened) {
            selector.wakeup();
            throw new RuntimeException("selector did not wake up");
        }

        if (!closed) {
            selector.close();
            throw new RuntimeException("selector did not close");
        }
    }
}

