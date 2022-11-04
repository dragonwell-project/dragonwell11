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

package com.alibaba.wisp.engine;

import jdk.internal.misc.JavaLangAccess;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.reflect.CallerSensitive;

import java.dyn.CoroutineSupport;

/**
 * An wrapper of {@link Thread} let every {@link WispTask} get different thread
 * object from {@link Thread#currentThread()}.
 * In this way, we make the listed class (not only but include) behave expected without
 * changing their code.
 * <p>
 * 1. {@link ThreadLocal}
 * 2. {@link java.util.concurrent.locks.AbstractQueuedSynchronizer} based synchronizer
 * 3. Netty's judgment of weather we are running in it's worker thread.
 */
class WispThreadWrapper extends Thread {

    WispThreadWrapper(WispTask task) {
        JLA.setWispTask(this, task);
        setName(task.getName());
    }

    @Override
    public CoroutineSupport getCoroutineSupport() {
        return JLA.getWispTask(this).carrier.thread.getCoroutineSupport();
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException();
    }

    @Override
    @CallerSensitive
    public ClassLoader getContextClassLoader() {
        return JLA.getWispTask(this).ctxClassLoader;
    }

    @Override
    public void setContextClassLoader(ClassLoader cl) {
        JLA.getWispTask(this).ctxClassLoader = cl;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return super.getStackTrace();
    }

    @Override
    public long getId() {
        return super.getId();
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return JLA.getWispTask(this).carrier.thread.getUncaughtExceptionHandler();
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        JLA.getWispTask(this).carrier.thread.setUncaughtExceptionHandler(eh);
    }

    @Override
    public String toString() {
        return "WispThreadWrapper{" +
                "wispTask=" + JLA.getWispTask(this) +
                '}';
    }

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

}