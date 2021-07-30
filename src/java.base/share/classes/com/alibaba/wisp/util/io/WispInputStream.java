package com.alibaba.wisp.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

// Make a nio channel implemented Socket's InputStream be like a plain socket's and yield on block

public class WispInputStream extends InputStream {

    private static WispEngineAccess WEA = SharedSecrets.getWispEngineAccess();

    protected final SocketChannel ch;
    protected Socket so;
    private ByteBuffer bb = null;
    private byte[] bs = null;           // Invoker's previous array
    private byte[] b1 = null;

    private ByteBuffer readAhead = null;

    public WispInputStream(SocketChannel ch, Socket so) {
        assert !ch.isBlocking();
        this.ch = ch;
        this.so = so;
    }

    @Override
    public int read() throws IOException {
        if (b1 == null)
            b1 = new byte[1];
        int n = this.read(b1);
        if (n == 1)
            return b1[0] & 0xff;
        return -1;
    }

    @Override
    public int read(byte[] bs, int off, int len)
            throws IOException {
        if (len <= 0 || off < 0 || off + len > bs.length) {
            if (len == 0) {
                return 0;
            }
            throw new ArrayIndexOutOfBoundsException();
        }

        ByteBuffer bb = ((this.bs == bs) ? this.bb : ByteBuffer.wrap(bs));

        bb.limit(Math.min(off + len, bb.capacity()));
        bb.position(off);
        this.bb = bb;
        this.bs = bs;
        return read(bb);
    }


    protected int read(ByteBuffer bb)
            throws IOException {
        int n;

        if (readAhead != null && readAhead.hasRemaining()) {
            if (bb.remaining() >= readAhead.remaining()) {
                n = readAhead.remaining();
                bb.put(readAhead);
                return n;
            } else {
                n = bb.remaining();
                for (int i = 0; i < n; i++) {
                    bb.put(readAhead.get());
                }
                return n;
            }
        }

        try {
            if ((n = ch.read(bb)) != 0) {
                return n;
            }

            if (so.getSoTimeout() > 0) {
                WEA.addTimer(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(so.getSoTimeout()));
            }

            do {
                WEA.registerEvent(ch, SelectionKey.OP_READ);
                WEA.park(-1);

                if (so.getSoTimeout() > 0 && WEA.isTimeout()) {
                    throw new SocketTimeoutException("time out");

                }
            } while ((n = ch.read(bb)) == 0);
        } finally {
            if (so.getSoTimeout() > 0) {
                WEA.cancelTimer();
            }
            WEA.unregisterEvent();
        }

        return n;
    }

    public int available() throws IOException {
        if (readAhead == null) {
            readAhead = ByteBuffer.allocate(4096);
        } else if (readAhead.hasRemaining()) {
            return readAhead.remaining();
        }

        readAhead.clear();
        ch.read(readAhead);
        readAhead.flip();

        return readAhead.remaining();
    }

    public void close() throws IOException {
        ch.close();
    }
}
