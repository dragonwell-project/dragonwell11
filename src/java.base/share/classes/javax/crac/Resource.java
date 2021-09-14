/*
 * Copyright (c) 2017, 2021, Azul Systems, Inc. All rights reserved.
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

/**
 * An interface for receiving checkpoint/restore notifications.
 *
 * <p>The class that is interested in receiving a checkpoint/restore notification
 * implements this interface, and the object created with that class is
 * registered with a {@code Context}, using {@code register} method.
 */
public interface Resource {

    /**
     * Invoked by a {@code Context} as a notification about checkpoint.
     *
     * @param context {@code Context} providing notification
     * @throws Exception if the method have failed
     */
    void beforeCheckpoint(Context<? extends Resource> context) throws Exception;

    /**
     * Invoked by a {@code Context} as a notification about restore.
     *
     * @param context {@code Context} providing notification
     * @throws Exception if the method have failed
     */
    void afterRestore(Context<? extends Resource> context) throws Exception;
}
