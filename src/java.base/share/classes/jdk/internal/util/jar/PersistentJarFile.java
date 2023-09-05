/*
 * Copyright (c) 2022, Azul Systems, Inc. All rights reserved.
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

package jdk.internal.util.jar;

import jdk.crac.Context;
import jdk.crac.Resource;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.crac.Core;
import jdk.internal.crac.JDKResource;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

public class PersistentJarFile extends JarFile implements JDKResource {
    // PersistentJarFile is <clinit>ed when loading classes on the module path;
    // when initializing the logger an implementation of logging is looked up through
    // service-loading and that causes a recursion in opening the module.
    // Therefore, we isolate the logger into a subclass and initialize only when needed.
    private static class LoggerContainer {
        private static final System.Logger logger = System.getLogger("jdk.crac");
    }

    public PersistentJarFile(File file, boolean b, int openRead, Runtime.Version runtimeVersion) throws IOException {
        super(file, b, openRead, runtimeVersion);
        if (jdk.crac.Configuration.checkpointEnabled()) {
            Core.getJDKContext().register(this);
        }
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        LoggerContainer.logger.log(System.Logger.Level.INFO, this.getName() + " is recorded as always available on restore");
        SharedSecrets.getJavaUtilZipFileAccess().beforeCheckpoint(this);
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // do nothing, no fixup required
    }

    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }
}
