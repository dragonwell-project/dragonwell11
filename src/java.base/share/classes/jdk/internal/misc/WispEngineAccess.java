package jdk.internal.misc;

import com.alibaba.wisp.engine.WispTask;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

public interface WispEngineAccess {

    WispTask getCurrentTask();

    void dispatch(Runnable runnable, String name);

    void eventLoop();

    void registerEvent(SelectableChannel ch, int events) throws IOException;

    void registerEpollEvent(int epFd) throws IOException;

    void unregisterEvent();

    void yieldOnBlocking();

    void addTimer(long deadlineNano);

    void cancelTimer();

    void sleep(long ms);

    void yield();

    boolean isThreadTask(WispTask task);

    boolean isTimeout();

    void park(long timeoutMs);

    void unpark(WispTask task);

    void destroy();

    boolean isAlive(WispTask task);

    void interrupt(WispTask task);

    boolean isInterrupted(WispTask task);

    boolean testInterruptedAndClear(WispTask task, boolean clear);

    <T> T runInCritical(CheckedSupplier<T> supplier);

    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Throwable;
    }

    boolean hasMoreTasks();

    boolean runningAsCoroutine(Thread t);

    boolean usingWispEpoll(Thread t);

    boolean ifPutToCurrentEngine();

    boolean ifProxySelector();

    boolean ifSpinSelector();

    boolean ifPutToManagedThread();

    boolean useThreadPoolLimit();

    String getThreadUsage(String threadName);

    boolean tryStartThreadAsWisp(Thread thread, Runnable target);
}
