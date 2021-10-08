/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import jdk.internal.misc.IOEventAccess;
import jdk.internal.misc.SharedSecrets;

import java.io.IOException;

public class IOEventRegister {

    static {
        SharedSecrets.setIOEventAccess(new IOEventAccess() {

            @Override
            public int eventCtlAdd() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int eventCtlDel() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int eventCtlMod() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int eventOneShot() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int noEvent() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long allocatePollArray(int count) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void freePollArray(long address) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getEvent(long address, int i) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getDescriptor(long eventAddress) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getEvents(long eventAddress) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int eventCreate() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int eventCtl(int epfd, int opcode, int fd, int events) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int eventWait(int epfd, long pollAddress, int numfds, int timeout) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void socketpair(int[] sv) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void interrupt(int fd) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void drain(int fd) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close(int fd) {
                throw new UnsupportedOperationException();
            }
        });
    }

}