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
 * @test Selector/wakeupAfterRestore
 * @summary check that the thread blocked by Selector.select() on checkpoint could be properly woken up after restore
 * @library /test/lib
 * @build Test
 * @run driver jdk.test.lib.crac.CracTest true
 * @run driver jdk.test.lib.crac.CracTest false
 */
public class Test implements CracTest {

    private final static long TIMEOUT = 3600_000; // looong timeout

    static boolean awakened;

    @CracTestArg
    boolean setTimeout;

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
                System.out.println(">> select, setTimeout = " + setTimeout);
                try {
                    awakened = false;
                    if (setTimeout) { selector.select(TIMEOUT); }
                    else { selector.select(); }
                    awakened = true;
                } catch (IOException e) { throw new RuntimeException(e); }
            }
        };
        Thread t = new Thread(r);
        t.start();
        Thread.sleep(1000);

        jdk.crac.Core.checkpointRestore();

        System.out.print(">> waking up: ");
        selector.wakeup();
        t.join();
        System.out.println("done");

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
