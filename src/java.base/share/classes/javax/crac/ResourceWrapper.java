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

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

class ResourceWrapper extends WeakReference<Resource> implements jdk.crac.Resource {
    private static WeakHashMap<Resource, ResourceWrapper> weakMap = new WeakHashMap<>();

    // Create strong reference to avoid losing the Resource.
    // It's set unconditionally in beforeCheckpoint and cleaned in afterRestore
    // (latter is called regardless of beforeCheckpoint result).
    private Resource strongRef;

    private final Context<Resource> context;

    public ResourceWrapper(Context<Resource> context, Resource resource) {
        super(resource);
        weakMap.put(resource, this);
        strongRef = null;
        this.context = context;
    }

    @Override
    public String toString() {
        Resource r = get();
        if (r != null) {
            return "ResourceWrapper[" + r + "]";
        } else {
            return "ResourceWrapper[null]";
        }
    }

    @Override
    public void beforeCheckpoint(jdk.crac.Context<? extends jdk.crac.Resource> context)
            throws Exception {
        Resource r = get();
        strongRef = r;
        if (r != null) {
            try {
                r.beforeCheckpoint(this.context);
            } catch (CheckpointException e) {
                Exception newException = new jdk.crac.CheckpointException();
                for (Throwable t : e.getSuppressed()) {
                    newException.addSuppressed(t);
                }
                throw newException;
            }
        }
    }

    @Override
    public void afterRestore(jdk.crac.Context<? extends jdk.crac.Resource> context) throws Exception {
        Resource r = get();
        strongRef = null;
        if (r != null) {
            try {
                r.afterRestore(this.context);
            } catch (RestoreException e) {
                Exception newException = new jdk.crac.RestoreException();
                for (Throwable t : e.getSuppressed()) {
                    newException.addSuppressed(t);
                }
                throw newException;
            }
        }
    }
}
