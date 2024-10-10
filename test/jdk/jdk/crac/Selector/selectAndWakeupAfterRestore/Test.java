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

import java.io.IOException;
import java.nio.channels.Selector;

/*
 * @test Selector/selectAndWakeupAfterRestore
 * @summary a trivial check that Selector.wakeup() after restore behaves as expected
 * @library /test/lib
 * @build Test
 * @run driver jdk.test.lib.crac.CracTest
 */
public class Test implements CracTest {
    @Override
    public void test() throws Exception {
        new CracBuilder().doCheckpointAndRestore();
    }

    private static void selectAndWakeup(Selector selector) throws java.io.IOException {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(7000);
                    System.out.println(">> waking up");
                    selector.wakeup();
                } catch (InterruptedException ie) { throw new RuntimeException(ie); }
            }
        }).start();

        System.out.println(">> selecting");
        selector.select();
    }

    @Override
    public void exec() throws Exception {

        Selector selector = Selector.open();

        selectAndWakeup(selector); // just in case

        jdk.crac.Core.checkpointRestore();

        selectAndWakeup(selector);

        selector.close();
    }
}
