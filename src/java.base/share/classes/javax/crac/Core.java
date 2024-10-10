/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
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

import jdk.crac.impl.OrderedContext;

/**
 * The coordination service.
 */
public class Core {

    /**
     * save the pseudo persistent file to image dir,
     * then restore to origin path
     */
    public static final int SAVE_RESTORE = 0x01;

    /**
     * save the pseudo persistent file to image dir,
     * but not restore.
     */
    public static final int SAVE_ONLY = 0x02;

    /**
     * If the file exist when restore, override the files
     */
    public static final int OVERRIDE_WHEN_RESTORE = 0x04;

    /**
     * copy the file from image dir to origin file path when restore
     */
    public static final int COPY_WHEN_RESTORE = 0x08;

    /**
     * create a symbol link when restore a file.
     * This should a better performance than COPY_WHEN_RESTORE if file is large
     */
    public static final int SYMLINK_WHEN_RESTORE = 0x10;

    /**
     * all flags set that test if given mode is valid.
     */
    private static final int ALL_FLAGS = SAVE_RESTORE | SAVE_ONLY | OVERRIDE_WHEN_RESTORE
            | COPY_WHEN_RESTORE | SYMLINK_WHEN_RESTORE;


    /** This class is not instantiable. */
    private Core() {
    }

    private static final Context<Resource> globalContext = new ContextWrapper(new OrderedContext());
    static {
        jdk.crac.Core.getGlobalContext().register(new ResourceWrapper(null, globalContext));
    }

    /**
     * Gets the global {@code Context} for checkpoint/restore notifications.
     *
     * @return the global {@code Context}
     */
    public static Context<Resource> getGlobalContext() {
        return globalContext;
    }

    /**
     * Requests checkpoint and returns upon a successful restore.
     * May throw an exception if the checkpoint or restore are unsuccessful.
     *
     * @throws CheckpointException if an exception occured during checkpoint
     * notification and the execution continues in the original Java instance.
     * @throws RestoreException if an exception occured during restore
     * notification and execution continues in a new Java instance.
     * @throws UnsupportedOperationException if checkpoint/restore is not
     * supported, no notification performed and the execution continues in
     * the original Java instance.
     */
    public static void checkpointRestore() throws
            CheckpointException,
            RestoreException {
        try {
            jdk.crac.Core.checkpointRestore();
        } catch (jdk.crac.CheckpointException e) {
            CheckpointException newException = new CheckpointException();
            for (Throwable t : e.getSuppressed()) {
                newException.addSuppressed(t);
            }
            throw newException;
        } catch (jdk.crac.RestoreException e) {
            RestoreException newException = new RestoreException();
            for (Throwable t : e.getSuppressed()) {
                newException.addSuppressed(t);
            }
            throw newException;
        }
    }

    /**
     * Pseudo persistent is the file that persistent when checkpoint.
     * This file can be modified after restore, but the change is discard when restore again.
     * Register the file path to JVM so that JVM can ignore the file check when checkpoint
     * @param absoluteFilePath the file path that need to register
     * @param mode control the action when checkpoint and restore pseudo persistent file.
     *             See the constants define in current class.
     */
    public static void registerPseudoPersistent(String absoluteFilePath, int mode) {
        if ((mode & ~ALL_FLAGS) != 0) {
            throw new IllegalArgumentException("Unknown mode 0x"
                    + Integer.toHexString(mode));
        }
        if ((mode & SAVE_RESTORE) != 0 && (mode & SAVE_ONLY) != 0) {
            throw new IllegalArgumentException("SAVE_RESTORE and SAVE_ONLY are exclusive");
        }
        if ((mode & COPY_WHEN_RESTORE) != 0 && (mode & SYMLINK_WHEN_RESTORE) != 0) {
            throw new IllegalArgumentException("COPY_WHEN_RESTORE and SYMLINK_WHEN_RESTORE are exclusive");
        }

        jdk.crac.Core.registerPseudoPersistent(absoluteFilePath, mode);
    }

    /**
     * Unregister the file that register in registerPseudoPersistent
     * @param absoluteFilePath the file path that need to unregister
     */
    public static void unregisterPseudoPersistent(String absoluteFilePath) {
        jdk.crac.Core.unregisterPseudoPersistent(absoluteFilePath);
    }

    /**
     * append path to app clasloader's classpath.
     * @param path the file need to append
     * @throws jdk.crac.CheckpointException this method can be called only when restore in progress
     */
    public static void appendToAppClassLoaderClassPath(String path) throws jdk.crac.CheckpointException {
        jdk.crac.Core.appendToAppClassLoaderClassPath(path);
    }
}
