/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.nio.ch;

import java.io.IOException;

import jdk.internal.misc.IOEventAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.Unsafe;

/**
 * Provides access to the Linux epoll facility.
 */

class EPoll {
    private EPoll() { }

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    /**
     * typedef union epoll_data {
     *     void *ptr;
     *     int fd;
     *     __uint32_t u32;
     *     __uint64_t u64;
     *  } epoll_data_t;
     *
     * struct epoll_event {
     *     __uint32_t events;
     *     epoll_data_t data;
     * }
     */
    private static final int SIZEOF_EPOLLEVENT   = eventSize();
    private static final int OFFSETOF_EVENTS     = eventsOffset();
    private static final int OFFSETOF_FD         = dataOffset();

    // opcodes
    static final int EPOLL_CTL_ADD  = 1;
    static final int EPOLL_CTL_DEL  = 2;
    static final int EPOLL_CTL_MOD  = 3;

    // events
    static final int EPOLLIN   = 0x1;
    static final int EPOLLOUT  = 0x4;

    // flags
    static final int EPOLLONESHOT   = (1 << 30);

    static final int ENOENT;

    /**
     * Allocates a poll array to handle up to {@code count} events.
     */
    static long allocatePollArray(int count) {
        return unsafe.allocateMemory(count * SIZEOF_EPOLLEVENT);
    }

    /**
     * Free a poll array
     */
    static void freePollArray(long address) {
        unsafe.freeMemory(address);
    }

    /**
     * Returns event[i];
     */
    static long getEvent(long address, int i) {
        return address + (SIZEOF_EPOLLEVENT*i);
    }

    /**
     * Returns event->data.fd
     */
    static int getDescriptor(long eventAddress) {
        return unsafe.getInt(eventAddress + OFFSETOF_FD);
    }

    /**
     * Returns event->events
     */
    static int getEvents(long eventAddress) {
        return unsafe.getInt(eventAddress + OFFSETOF_EVENTS);
    }

    // -- Native methods --

    private static native int eventSize();

    private static native int eventsOffset();

    private static native int dataOffset();

    static native int create() throws IOException;

    static native int ctl(int epfd, int opcode, int fd, int events);

    static native int wait(int epfd, long pollAddress, int numfds, int timeout)
        throws IOException;

    static native int errnoENOENT();

    static {
        IOUtil.load();
        ENOENT = errnoENOENT();
        SharedSecrets.setIOEventAccess(new IOEventAccess() {
            @Override
            public int eventCtlAdd() {
                return EPoll.EPOLL_CTL_ADD;
            }

            @Override
            public int eventCtlDel() {
                return EPoll.EPOLL_CTL_DEL;
            }

            @Override
            public int eventCtlMod() {
                return EPoll.EPOLL_CTL_MOD;
            }

            @Override
            public int eventOneShot() {
                return EPoll.EPOLLONESHOT;
            }

            @Override
            public int noEvent() {
                return EPoll.ENOENT;
            }

            @Override
            public long allocatePollArray(int count) {
                return EPoll.allocatePollArray(count);
            }

            @Override
            public void freePollArray(long address) {
                EPoll.freePollArray(address);
            }

            @Override
            public long getEvent(long address, int i) {
                return EPoll.getEvent(address, i);
            }

            @Override
            public int getDescriptor(long eventAddress) {
                return EPoll.getDescriptor(eventAddress);
            }

            @Override
            public int getEvents(long eventAddress) {
                return EPoll.getEvents(eventAddress);
            }

            @Override
            public int eventCreate() throws IOException {
                return EPoll.create();
            }

            @Override
            public int eventCtl(int epfd, int opcode, int fd, int events) {
                return EPoll.ctl(epfd, opcode, fd, events);
            }

            @Override
            public int eventWait(int epfd, long pollAddress, int numfds, int timeout) throws IOException {
                return EPoll.wait(epfd, pollAddress, numfds, timeout);
            }

            @Override
            public void socketpair(int[] sv) throws IOException {
                long fds = IOUtil.makePipe(false);
                sv[0] = (int) (fds >>> 32);
                sv[1] = (int) fds;
            }

            @Override
            public void interrupt(int fd) throws IOException {
                IOUtil.write1(fd, (byte)0);
            }

            @Override
            public void drain(int fd) throws IOException {
                IOUtil.drain1(fd);
            }

            @Override
            public void close(int fd) {
                try {
                    FileDispatcherImpl.closeIntFD(fd);
                } catch (IOException e) {
                    // pass
                }
            }
        });
    }
}
