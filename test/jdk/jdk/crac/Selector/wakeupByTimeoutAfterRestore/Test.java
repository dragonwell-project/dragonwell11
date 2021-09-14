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

import java.nio.channels.Selector;
import java.io.IOException;

/*
 * @test Selector/wakeupByTimeoutAfterRestore
 * @summary check that the Selector selected before the checkpoint,
 *          will wake up by timeout after the restore
 * @library /test/lib
 * @build Test
 * @run driver jdk.test.lib.crac.CracTest
 */
public class Test implements CracTest {

    private final static long TIMEOUT = 40_000; // 40 seconds

    static boolean awakened = false;

    @Override
    public void test() throws Exception {
        new CracBuilder().doCheckpointAndRestore();
    }

    @Override
    public void exec() throws Exception {
        Selector selector = Selector.open();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    selector.select(TIMEOUT);
                    awakened = true;
                } catch (IOException e) { throw new RuntimeException(e); }
            }
        };
        Thread t = new Thread(r);
        t.start();
        Thread.sleep(1000);

        jdk.crac.Core.checkpointRestore();

        t.join();
        if (!awakened) { throw new RuntimeException("not awakened!"); }

        // check that the selector works as expected

        if (!selector.isOpen()) { throw new RuntimeException("the selector must be open"); }

        selector.wakeup();
        selector.select();

        selector.selectNow();
        selector.select(200);
        selector.close();
    }
}
