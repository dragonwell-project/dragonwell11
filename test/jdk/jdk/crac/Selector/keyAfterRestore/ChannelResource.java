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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

class ChannelResource implements Resource {

    private SocketChannel channel;
    private SelectionKey key;
    private Selector selector;

    private Object att = new Integer(123);

    public ChannelResource() {
        Core.getGlobalContext().register(this);
    }

    public void open() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(false);
    }

    public void register(Selector selector) throws IOException {
        key = channel.register(selector, SelectionKey.OP_CONNECT);
        key.attach(att);
        this.selector = selector;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws IOException {

        channel.socket().close(); // close the channel => cancel the key
        check(!channel.isOpen(), "the channel should not be open");
        selector.select(100); // causes the channel deregistration
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {

        check(key.selector().equals(selector), "invalid key.selector()");
        check(key.channel().equals(channel), "invalid key.channel()");

        // the key is cancelled
        check(!key.isValid(), "expected: key.isValid() == false");

        boolean caught = false;
        try {
            key.readyOps();
        } catch (CancelledKeyException e) {
            caught = true;
        }
        check(caught, "expected CancelledKeyException is missing");

        caught = false;
        try {
            key.interestOps();
        } catch (CancelledKeyException e) {
            caught = true;
        }
        check(caught, "expected CancelledKeyException is missing");

        caught = false;
        try {
            key.interestOps(SelectionKey.OP_CONNECT);
        } catch (CancelledKeyException e) {
            caught = true;
        }
        check(caught, "expected CancelledKeyException is missing");

        caught = false;
        try {
            key.readyOps();
        } catch (CancelledKeyException e) {
            caught = true;
        }
        check(caught, "expected CancelledKeyException is missing");

        caught = false;
        try {
            key.isReadable();
        } catch (CancelledKeyException e) {
            caught = true;
        }
        check(caught, "expected CancelledKeyException is missing");

        caught = false;
        try {
            key.isWritable();
        } catch (CancelledKeyException e) {
            caught = true;
        }
        check(caught, "expected CancelledKeyException is missing");

        caught = false;
        try {
            key.isConnectable();
        } catch (CancelledKeyException e) {
            caught = true;
        }
        check(caught, "expected CancelledKeyException is missing");

        caught = false;
        try {
            key.isAcceptable();
        } catch (CancelledKeyException e) {
            caught = true;
        }
        check(caught, "expected CancelledKeyException is missing");

        check(att.equals(key.attachment()), "invalid key.attachment()");

        key.cancel(); // try just in case

        // register again
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            key = channel.register(selector, SelectionKey.OP_READ);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // to check after restore
    public void checkKey() {

        check(key.isValid(), "key must be valid");

        check(key.selector().equals(selector), "invalid key.selector()");
        check(key.channel().equals(channel), "invalid key.channel()");

        key.isReadable(); // just call, cannot set "ready" state manually
        check(!key.isWritable(), "invalid key.isWritable()");
        check(!key.isConnectable(), "invalid key.isConnectable()");
        check(!key.isAcceptable(), "invalid key.isAcceptable()");

        check(key.interestOps() == SelectionKey.OP_READ, "invalid key.interestOps()");

        System.out.println(">> ready >> " + key.readyOps());

        check(key.attachment() == null, "key.attachment() expected to be null");

        key.cancel(); // try just in case
    }

    private void check(boolean b, String msg) {
        if (!b) {
            throw new RuntimeException(msg);
        }
    }
}
