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

package jdk.crac;

/**
 * A {@code Resource} that allows other {@code Resource}s to be registered with it.
 *
 * <p>{@code Context} implementation overrides {@code beforeCheckpoint} and {@code afterRestore}, defining how the notification about checkpoint and restore will be distributed by the {@code Context} hierarchy.
 *
 * <p>A {@code Context} implementor is encouraged to respect properties of the global {@code Context}.
 */
public abstract class Context<R extends Resource> implements Resource {

    /** Creates a {@code Context}.
     */
    protected Context() {
    }

    @Override
    public abstract void beforeCheckpoint(Context<? extends Resource> context)
            throws CheckpointException;

    @Override
    public abstract void afterRestore(Context<? extends Resource> context)
            throws RestoreException;

    /**
     * Registers a {@code Resource} with this {@code Context}.
     *
     * @param resource {@code Resource} to be registered.
     * @throws NullPointerException if {@code resource} is {@code null}
     */
    public abstract void register(R resource);
}
