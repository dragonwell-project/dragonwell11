/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileDescriptor;
import java.io.IOException;
import jdk.internal.misc.SharedSecrets;

import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.crac.Context;
import jdk.crac.Resource;
import jdk.internal.misc.JavaIOFileDescriptorAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.crac.Core;
import jdk.internal.crac.JDKResource;
import jdk.internal.crac.JDKResource.Priority;

class FileDispatcherImpl extends FileDispatcher {
    static class ResourceProxy implements JDKResource {
        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            FileDispatcherImpl.beforeCheckpoint();
        }

        @Override
        public void afterRestore(Context<? extends Resource> context)
                throws IOException {
            FileDispatcherImpl.afterRestore();
        }

        @Override
        public Priority getPriority() {
            return Priority.NORMAL;
        }
    }

    static Object closeLock = new Object();
    static boolean forceNonDeferedClose;
    static int closeCnt;

    static ResourceProxy resourceProxy = new ResourceProxy();

    static {
        IOUtil.load();
        init();
        Core.getJDKContext().register(resourceProxy);
    }

    private static final JavaIOFileDescriptorAccess fdAccess =
            SharedSecrets.getJavaIOFileDescriptorAccess();

    FileDispatcherImpl() {
    }

    int read(FileDescriptor fd, long address, int len) throws IOException {
        if (SharedSecrets.getWispFileSyncIOAccess() != null && SharedSecrets.getWispFileSyncIOAccess().usingAsyncFileIO()) {
            return SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> read0(fd, address, len));
        } else {
            return read0(fd, address, len);
        }
    }

    int pread(FileDescriptor fd, long address, int len, long position)
            throws IOException
    {
        if (SharedSecrets.getWispFileSyncIOAccess() != null && SharedSecrets.getWispFileSyncIOAccess().usingAsyncFileIO()) {
            return SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> pread0(fd, address, len, position));
        } else {
            return pread0(fd, address, len, position);
        }
    }

    long readv(FileDescriptor fd, long address, int len) throws IOException {
        if (SharedSecrets.getWispFileSyncIOAccess() != null && SharedSecrets.getWispFileSyncIOAccess().usingAsyncFileIO()) {
            return SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> readv0(fd, address, len));
        } else {
            return readv0(fd, address, len);
        }
    }

    int write(FileDescriptor fd, long address, int len) throws IOException {
        if (SharedSecrets.getWispFileSyncIOAccess() != null && SharedSecrets.getWispFileSyncIOAccess().usingAsyncFileIO()) {
            return SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> write0(fd, address, len));
        } else {
            return write0(fd, address, len);
        }
    }

    int pwrite(FileDescriptor fd, long address, int len, long position)
        throws IOException
    {
        if (SharedSecrets.getWispFileSyncIOAccess() != null && SharedSecrets.getWispFileSyncIOAccess().usingAsyncFileIO()) {
            return SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> pwrite0(fd, address, len, position));
        } else {
            return pwrite0(fd, address, len, position);
        }
    }

    long writev(FileDescriptor fd, long address, int len)
        throws IOException
    {
        if (SharedSecrets.getWispFileSyncIOAccess() != null && SharedSecrets.getWispFileSyncIOAccess().usingAsyncFileIO()) {
            return SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> writev0(fd, address, len));
        } else {
            return writev0(fd, address, len);
        }
    }

    long seek(FileDescriptor fd, long offset) throws IOException {
        return seek0(fd, offset);
    }

    int force(FileDescriptor fd, boolean metaData) throws IOException {
        if (SharedSecrets.getWispFileSyncIOAccess() != null && SharedSecrets.getWispFileSyncIOAccess().usingAsyncFileIO()) {
            return SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> force0(fd, metaData));
        } else {
            return force0(fd, metaData);
        }
    }

    int truncate(FileDescriptor fd, long size) throws IOException {
        if (SharedSecrets.getWispFileSyncIOAccess() != null && SharedSecrets.getWispFileSyncIOAccess().usingAsyncFileIO()) {
            return SharedSecrets.getWispFileSyncIOAccess().executeAsAsyncFileIO(() -> truncate0(fd, size));
        } else {
            return truncate0(fd, size);
        }
    }

    long size(FileDescriptor fd) throws IOException {
        return size0(fd);
    }

    int lock(FileDescriptor fd, boolean blocking, long pos, long size,
             boolean shared) throws IOException
    {
        return lock0(fd, blocking, pos, size, shared);
    }

    void release(FileDescriptor fd, long pos, long size) throws IOException {
        release0(fd, pos, size);
    }

    void close(FileDescriptor fd) throws IOException {
        fdAccess.close(fd);
    }

    void preClose(FileDescriptor fd) throws IOException {
        boolean doPreclose = true;
        synchronized (closeLock) {
            if (forceNonDeferedClose) {
                doPreclose = false;
            }
            if (doPreclose) {
                ++closeCnt;
            }
        }

        if (!doPreclose) {
            return;
        }

        try {
            preClose0(fd);
        } finally {
            synchronized (closeLock) {
                closeCnt--;
                if (forceNonDeferedClose && closeCnt == 0) {
                    closeLock.notifyAll();
                }
            }
        }
    }

    FileDescriptor duplicateForMapping(FileDescriptor fd) {
        // file descriptor not required for mapping operations; okay
        // to return invalid file descriptor.
        return new FileDescriptor();
    }

    boolean canTransferToDirectly(java.nio.channels.SelectableChannel sc) {
        return true;
    }

    boolean transferToDirectlyNeedsPositionLock() {
        return false;
    }

    int setDirectIO(FileDescriptor fd, String path) {
        int result = -1;
        try {
            result = setDirect0(fd);
        } catch (IOException e) {
            throw new UnsupportedOperationException
                ("Error setting up DirectIO", e);
        }
        return result;
    }

    static void beforeCheckpoint() throws InterruptedException {
        synchronized (closeLock) {
            forceNonDeferedClose = true;
            while (closeCnt != 0) {
                closeLock.wait();
            }
            beforeCheckpoint0();
        }
    }

    static void afterRestore() throws IOException {
        synchronized (closeLock) {
            afterRestore0();
            forceNonDeferedClose = false;
        }
    }

    // -- Native methods --

    static native int read0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int pread0(FileDescriptor fd, long address, int len,
                             long position) throws IOException;

    static native long readv0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int write0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int pwrite0(FileDescriptor fd, long address, int len,
                             long position) throws IOException;

    static native long writev0(FileDescriptor fd, long address, int len)
        throws IOException;

    static native int force0(FileDescriptor fd, boolean metaData)
        throws IOException;

    static native long seek0(FileDescriptor fd, long offset)
        throws IOException;

    static native int truncate0(FileDescriptor fd, long size)
        throws IOException;

    static native long size0(FileDescriptor fd) throws IOException;

    static native int lock0(FileDescriptor fd, boolean blocking, long pos,
                            long size, boolean shared) throws IOException;

    static native void release0(FileDescriptor fd, long pos, long size)
        throws IOException;

    // Shared with SocketDispatcher and DatagramDispatcher but
    // NOT used by FileDispatcherImpl
    static native void close0(FileDescriptor fd) throws IOException;

    static native void preClose0(FileDescriptor fd) throws IOException;

    static native void closeIntFD(int fd) throws IOException;

    static native int setDirect0(FileDescriptor fd) throws IOException;

    static native void init();

    static native void beforeCheckpoint0();

    static native void afterRestore0() throws IOException;
}
