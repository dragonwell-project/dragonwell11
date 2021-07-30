package com.alibaba.wisp.util.io;


import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * For the IO intensive workloads, we need to wakeup IO threads frequently via
 * {@link Selector#wakeup()}, which is an expensive operation(lock, IO).
 * <p>
 * So we use a "sleep" strategy to solve this problem:
 * If IO thread is busy, the call on  {@link Selector#wakeup()} is ignored as
 * IO thread will be definitely wakened  by IO operations self, which is already registered in {@link Selector}.
 */
public class SleepSelector extends AbstractSelector {

    private static final Field cancelledKeysField;
    private static final Method registerMethod;
    private static int ENTER_SLEEP_CNT;
    private static int EXIT_SLEEP_CNT;
    private static int SAFE_INTERVAL;

    static {
        try {
            cancelledKeysField = AbstractSelector.class.getDeclaredField("cancelledKeys");
            cancelledKeysField.setAccessible(true);
            registerMethod = AbstractSelector.class.getDeclaredMethod("register",
                    AbstractSelectableChannel.class, int.class, Object.class);
            registerMethod.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
        java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        ENTER_SLEEP_CNT = Integer.getInteger("com.alibaba.selector.sleep.onCnt", 18);
                        EXIT_SLEEP_CNT = Integer.getInteger("com.alibaba.selector.sleep.offCnt", 12);
                        SAFE_INTERVAL = Integer.getInteger("com.alibaba.selector.safe.interval", 120);
                        return null;
                    }
                });
    }

    // sleep selector algorithm control variables
    private volatile boolean needKick = true; // should we wakeup selector?
    private int sleepInterval = 1;
    private int continuousEvent = 0;
    private int continuousNonEvent = 0;

    private final AbstractSelector target;

    @SuppressWarnings("unchecked")
    public SleepSelector(AbstractSelector target) {
        super(target.provider());
        this.target = target;
        try {
            Set<SelectionKey> targetCk = (Set<SelectionKey>) cancelledKeysField.get(target);
            cancelledKeysField.set(this, new AbstractSet<SelectionKey>() {
                @Override
                public boolean add(SelectionKey sk) {
                    synchronized (targetCk) {
                        return targetCk.add(sk);
                    }
                }

                @Override
                public Iterator<SelectionKey> iterator() {
                    return Collections.emptyIterator();
                }

                @Override
                public int size() {
                    return 0;
                }
            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void implCloseSelector() throws IOException {
        target.close();
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        try {
            return (SelectionKey) registerMethod.invoke(target, ch, ops, att);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Set<SelectionKey> keys() {
        return target.keys();
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return target.selectedKeys();
    }

    @Override
    public int selectNow() throws IOException {
        return target.selectNow();
    }

    @Override
    public int select(long timeout) throws IOException {
        return updateKickStatus(target.select(Math.min(timeout, sleepInterval)));
    }

    @Override
    public int select() throws IOException {
        return updateKickStatus(target.select(sleepInterval));
    }

    private int updateKickStatus(final int nKeys) {
        if (nKeys > 0) {
            sleepInterval = 1;
            continuousNonEvent = 0;
            if (++continuousEvent >= ENTER_SLEEP_CNT && needKick) {
                needKick = false;
            }
        } else {
            continuousEvent = 0;
            if (++continuousNonEvent >= EXIT_SLEEP_CNT && !needKick) {
                needKick = true;
            }
            if (continuousNonEvent >= EXIT_SLEEP_CNT + 2
                    // +2 is enough to ensure needKick = true is already seen by other threads.
                    && sleepInterval == 1) {
                // we're really not busy, use longer sleep interval to save CPU
                sleepInterval = SAFE_INTERVAL;
            }
        }
        return nKeys;
    }

    @Override
    public Selector wakeup() {
        if (needKick) target.wakeup();
        return this;
    }
}
