package com.alibaba.wisp.engine;

class TaskDispatcher implements StealAwareRunnable {
    private final ClassLoader ctxClassLoader;
    private final long enqueueTime;
    private final Runnable target;
    private final String name;
    private final Thread thread;

    TaskDispatcher(ClassLoader ctxClassLoader, Runnable target, String name, Thread thread) {
        this.ctxClassLoader = ctxClassLoader;
        this.enqueueTime = WispEngine.getNanoTime();
        this.target = target;
        this.name = name;
        this.thread = thread;
    }

    @Override
    public void run() {
        WispCarrier current = WispCarrier.current();
        current.countEnqueueTime(enqueueTime);
        current.runTaskInternal(target, name, thread,
                ctxClassLoader == null ? current.current.ctxClassLoader : ctxClassLoader);
    }
}
