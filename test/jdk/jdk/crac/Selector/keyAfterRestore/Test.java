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


import java.nio.channels.*;
import java.io.IOException;
import jdk.crac.*;
import jdk.test.lib.crac.CracBuilder;
import jdk.test.lib.crac.CracTest;
import jdk.test.lib.crac.CracTestArg;

/*
 * @test Selector/keyAfterRestore
 * @summary a trivial test for SelectionKey's state after restore
 * @library /test/lib
 * @build ChannelResource
 * @build Test
 * @run driver/timeout=30 jdk.test.lib.crac.CracTest true
 * @run driver/timeout=30 jdk.test.lib.crac.CracTest false
 */
public class Test implements CracTest {
    @CracTestArg
    boolean openSelectorAtFirst;

    @Override
    public void test() throws Exception {
        new CracBuilder().doCheckpointAndRestore();
    }

    @Override
    public void exec() throws Exception {
        ChannelResource ch;
        Selector selector = null;

        // check various order (see ZE-970)
        if (openSelectorAtFirst) { selector = Selector.open(); }

        ch = new ChannelResource();
        ch.open();

        if (!openSelectorAtFirst) { selector = Selector.open(); }

        ch.register(selector);

        try {
            Core.checkpointRestore();
        } catch (CheckpointException | RestoreException e) {
            e.printStackTrace();
            throw e;
        }

        Thread.sleep(200);

        ch.checkKey();

        selector.close();
    }
}

