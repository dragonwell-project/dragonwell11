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

package jdk.test.jdk.crac;

import jdk.crac.*;

/**
 * @test
 * @compile ResourceTest.java
 */
public class ResourceTest {
    static class CRResource implements Resource {
        String id;
        boolean[] throwCond;
        int nCalls = 0;
        CRResource(String id, boolean... throwCond) {
            this.id = id;
            this.throwCond = throwCond;
        }

        void maybeException(String callId) throws Exception {
            boolean t = nCalls < throwCond.length ? throwCond[nCalls] : throwCond[throwCond.length - 1];
            System.out.println(id + " " + callId + "(" + nCalls + ") throw? " + t);
            ++nCalls;
            if (t) {
                throw new RuntimeException(id);
            }
        }

        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
            maybeException("beforeCheckpoint");
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws Exception {
            maybeException("afterRestore");
        }
    }

    static class SingleContext extends Context<Resource> {
        private Resource r;

        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws CheckpointException {
            try {
                r.beforeCheckpoint(this);
            } catch (Exception e) {
                CheckpointException newException = new CheckpointException();
                newException.addSuppressed(e);
                throw newException;
            }
        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws RestoreException {
            try {
                r.afterRestore(this);
            } catch (Exception e) {
                RestoreException newException = new RestoreException();
                newException.addSuppressed(e);
                throw newException;
            }

        }

        @Override
        public void register(Resource r) {
            this.r = r;
        }

        public SingleContext(Resource r) {
            register(r);
        }
    }

    static public void main(String[] args) throws Exception {
        Core.getGlobalContext().register(
            new CRResource("One", true, false));
        Core.getGlobalContext().register(
            new SingleContext(
                new CRResource("Two", false, true, false, true)));
        //System.gc();
        int tries = 2;
        for (int i = 0; i < 2; ++i) {
            try {
                jdk.crac.Core.checkpointRestore();
            } catch (CheckpointException e) {
                e.printStackTrace();
            } catch (RestoreException e) {
                e.printStackTrace();
            }
        }
        System.out.println("DONE");
    }
}
