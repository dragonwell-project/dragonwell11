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
