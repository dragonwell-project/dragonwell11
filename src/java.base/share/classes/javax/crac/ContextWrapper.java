/*
 * Copyright (c) 2019, 2021, Azul Systems, Inc. All rights reserved.
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.crac;

class ContextWrapper extends Context<Resource> {
    private final jdk.crac.Context<jdk.crac.Resource> context;

    public ContextWrapper(jdk.crac.Context<jdk.crac.Resource> context) {
        this.context = context;
    }

    private static jdk.crac.Context<? extends jdk.crac.Resource> convertContext(
            Context<? extends Resource> context) {
        return context instanceof ContextWrapper ?
                ((ContextWrapper)context).context :
                null;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context)
            throws CheckpointException {
        try {
            this.context.beforeCheckpoint(convertContext(context));
        } catch (jdk.crac.CheckpointException e) {
            CheckpointException newException = new CheckpointException();
            for (Throwable t : e.getSuppressed()) {
                newException.addSuppressed(t);
            }
            throw newException;
        }
    }

    @Override
    public void afterRestore(Context<? extends Resource> context)
            throws RestoreException {
        try {
            this.context.afterRestore(convertContext(context));
        } catch (jdk.crac.RestoreException e) {
            RestoreException newException = new RestoreException();
            for (Throwable t : e.getSuppressed()) {
                newException.addSuppressed(t);
            }
            throw newException;
        }
    }

    @Override
    public void register(Resource r) {
        ResourceWrapper wrapper = new ResourceWrapper(this, r);
        context.register(wrapper);
    }

    @Override
    public String toString() {
        return "ContextWrapper[" + context.toString() + "]";
    }
}

