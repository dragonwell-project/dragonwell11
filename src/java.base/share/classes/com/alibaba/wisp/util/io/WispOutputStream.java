package com.alibaba.wisp.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispEngineAccess;

// Make a nio channel implemented Socket's OutputStream be like a plain socket's and yield on block

public class WispOutputStream extends OutputStream {

    private static WispEngineAccess WEA = SharedSecrets.getWispEngineAccess();

    protected final SocketChannel ch;
    protected final Socket so;
    private ByteBuffer bb = null;
    private byte[] bs = null;       // Invoker's previous array
    private byte[] b1 = null;


    public WispOutputStream(SocketChannel ch, Socket so) {
        assert !ch.isBlocking();
        this.ch = ch;
        this.so = so;
    }

    @Override
    public void write(int b) throws IOException {
        if (b1 == null)
            b1 = new byte[1];
        b1[0] = (byte) b;
        this.write(b1);
    }

    @Override
    public void write(byte[] bs, int off, int len)
            throws IOException
    {
        if (len <= 0 || off < 0 || off + len > bs.length) {
            if (len == 0) {
                return;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        ByteBuffer bb = ((this.bs == bs) ? this.bb : ByteBuffer.wrap(bs));
        bb.limit(Math.min(off + len, bb.capacity()));
        bb.position(off);
        this.bb = bb;
        this.bs = bs;

        write(bb);
    }

    protected void write(ByteBuffer bb)
            throws IOException {

        try {
            int writeLen = bb.remaining();
            if (ch.write(bb) == writeLen) {
                return;
            }

            if (so.getSoTimeout() > 0) {
                WEA.addTimer(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(so.getSoTimeout()));
            }

            do {
                WEA.registerEvent(ch, SelectionKey.OP_WRITE);
                WEA.park(-1);

                if (so.getSoTimeout() > 0 && WEA.isTimeout()) {
                    throw new SocketTimeoutException("time out");
                }
                ch.write(bb);
            } while (bb.remaining() > 0);
        } finally {
            if (so.getSoTimeout() > 0) {
                WEA.cancelTimer();
            }
            WEA.unregisterEvent();
        }
    }

    public void close() throws IOException {
        ch.close();
    }

}
