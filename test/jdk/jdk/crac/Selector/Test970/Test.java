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
 * @test Selector/Test970
 * @summary a regression test for ZE-970 ("a channel deregistration
 *          is locked depending on mutual order of selector and channel creation")
 * @library /test/lib
 * @build ChannelResource
 * @build Test
 * @run driver jdk.test.lib.crac.CracTest SELECT_NOW true
 * @run driver jdk.test.lib.crac.CracTest SELECT_NOW false
 * @run driver jdk.test.lib.crac.CracTest SELECT true
 * @run driver jdk.test.lib.crac.CracTest SELECT false
 * @run driver jdk.test.lib.crac.CracTest SELECT_TIMEOUT true
 * @run driver jdk.test.lib.crac.CracTest SELECT_TIMEOUT false
 */
public class Test implements CracTest {
    @CracTestArg(0)
    ChannelResource.SelectionType selType;

    @CracTestArg(1)
    boolean openSelectorAtFirst;

    @Override
    public void test() throws Exception {
        new CracBuilder().doCheckpointAndRestore();
    }

    @Override
    public void exec() throws Exception {

        if (openSelectorAtFirst) {

            Selector selector = Selector.open();
            ChannelResource ch = new ChannelResource(selType);
            ch.open();
            ch.register(selector);

            Core.checkpointRestore();

            selector.close();

        } else { // try in other order (see ZE-970)

            ChannelResource ch = new ChannelResource(selType);
            ch.open();
            Selector selector = Selector.open();
            ch.register(selector);

            Core.checkpointRestore();

            selector.close();
        }
    }
}
