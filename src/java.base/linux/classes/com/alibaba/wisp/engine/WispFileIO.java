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

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.WispFileSyncIOAccess;


enum WispFileIO {
    INSTANCE;

    private static volatile boolean wispFileWorkerStarted = false;

    static void setWispFileSyncIOIOAccess() {
        if (SharedSecrets.getWispFileSyncIOAccess() == null) {
            SharedSecrets.setWispFileSyncIOAccess(new WispFileSyncIOAccess() {
                @Override
                public boolean usingAsyncFileIO() {
                    return wispFileWorkerStarted
                            && WispEngine.runningAsCoroutine(Thread.currentThread());
                }

                @Override
                public <T> T executeAsAsyncFileIO(Callable<T> command) throws IOException {
                    return WispFileIO.INSTANCE.invokeIOTask(command);
                }
            });
        }
    }

    /*
     * Initialize the WispFileIO class, called after System.initializeSystemClass by VM.
     **/
    static void initializeWispFileIOClass() {
        try {
            Class.forName(WispFileIO.class.getName());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static void startWispFileIODaemon() {
        WispFileIO.INSTANCE.startDaemon(WispEngine.daemonThreadGroup);
        setWispFileSyncIOIOAccess();
    }


    private ExecutorService executor;

    private ThreadGroup threadGroup;

    void startDaemon(ThreadGroup g) {
        threadGroup = g;
        this.executor = new ThreadPoolExecutor(WispConfiguration.WISP_FILE_IO_WORKER_CORE,
                WispConfiguration.WISP_FILE_IO_WORKER_MAX, Long.MAX_VALUE,
                TimeUnit.SECONDS, new LinkedBlockingDeque<>(), new WispFileIOThreadPoolFactory());
        wispFileWorkerStarted = true;
    }

    public <T> T invokeIOTask(Callable<T> command) throws IOException {
        Future<T> future = submitIOTask(command);
        T result;
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    result = future.get();
                    return result;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException) {
                        throw (IOException) cause;
                    } else if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else if (cause instanceof Error) {
                        throw (Error) cause;
                    } else {
                        throw new Error(e);
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    <T> Future<T> submitIOTask(Callable<T> command) {
        return executor.submit(command);
    }

    private class WispFileIOThreadPoolFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final static String namePrefix = "Wisp-FIO-worker-thread-";

        WispFileIOThreadPoolFactory() {
        }

        @Override
        public Thread newThread(Runnable r) {
            assert threadGroup != null;
            Thread t = new Thread(threadGroup, r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
