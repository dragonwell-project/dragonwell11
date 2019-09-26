package jdk.internal.misc;


import com.alibaba.wisp.engine.WispTask;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.atomic.AtomicReference;

public interface WispEngineAccess {

    WispTask getCurrentTask();

    void dispatch(Runnable runnable, String name);

    void eventLoop();

    void registerEvent(SelectableChannel ch, int events) throws IOException;

    void unregisterEvent();

    int epollWait(int epfd, long pollArray, int arraySize, long timeout,
                  AtomicReference<Object> status, final Object INTERRUPTED) throws IOException;

    void interruptEpoll(AtomicReference<Object> status, Object INTERRUPTED, int interruptFD);

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

    boolean usingWispEpoll();

    boolean ifPutToCurrentEngine();

    boolean ifProxySelector();

    boolean ifSpinSelector();

    boolean ifPutToManagedThread();

    boolean isAllThreadAsWisp();

    boolean useThreadPoolLimit();

    String getThreadUsage(String threadName);

    boolean tryStartThreadAsWisp(Thread thread, Runnable target);

    boolean useDirectSelectorWakeup();

    boolean enableSocketLock();
}
