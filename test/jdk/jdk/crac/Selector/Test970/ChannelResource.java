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

import jdk.crac.Context;
import jdk.crac.Core;
import jdk.crac.Resource;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

class ChannelResource implements Resource {

    public enum SelectionType {
        SELECT,
        SELECT_TIMEOUT,
        SELECT_NOW
    };

    private SocketChannel channel;
    private SelectionKey key;
    private Selector selector;

    private final SelectionType selType;

    public ChannelResource(SelectionType selType) {
        this.selType = selType;
        Core.getGlobalContext().register(this);
    }

    public void open() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(false);
    }

    public void register(Selector selector) throws IOException {
        key = channel.register(selector, SelectionKey.OP_READ);
        this.selector = selector;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws IOException {

        channel.socket().close();

        // causes the channel deregistration
        if (selType == SelectionType.SELECT_NOW) {
            selector.selectNow();
        } else if (selType == SelectionType.SELECT_TIMEOUT) {
            selector.select(500);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        selector.wakeup();
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
            }).start();

            selector.select();
        }
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {
    }
}
