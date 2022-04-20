package sun.management;

import com.alibaba.wisp.engine.WispTask;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import java.lang.management.ThreadInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;


public class WispThreadCompositeData extends LazyCompositeData {
    private final WispTask task;

    public WispThreadCompositeData(WispTask wispTask) {
        task = wispTask;
    }

    @Override
    protected CompositeData getCompositeData() {
        Map<String, Object> items = new HashMap<>();
        Thread t = task != null ? task.getThreadWrapper() : null;
        Object parkBlocker = LockSupport.getBlocker(t);
        items.put(THREAD_ID,        t != null ? t.getId() : 0L);
        items.put(THREAD_NAME,      t != null ? t.getName() : "");
        items.put(THREAD_STATE,     t != null ? t.getState().name() : "");
        items.put(LOCK_NAME,        parkBlocker == null ? "" : parkBlocker.toString());
        items.put(BLOCKED_TIME,     0L);
        items.put(BLOCKED_COUNT,    0L);
        items.put(WAITED_TIME,      0L);
        items.put(WAITED_COUNT,     0L);
        items.put(LOCK_OWNER_ID,    0L);
        items.put(LOCK_OWNER_NAME,  "");
        items.put(STACK_TRACE,      new CompositeData[0]);
        items.put(SUSPENDED,        false);
        items.put(IN_NATIVE,        false);
        items.put(LOCKED_MONITORS,  new CompositeData[0]);
        items.put(LOCKED_SYNCS,     new CompositeData[0]);
        items.put(LOCK_INFO,        null);
        items.put(DAEMON,           t == null && t.isDaemon());
        items.put(PRIORITY,         t != null ? t.getPriority() : 0);

        try {
            return new CompositeDataSupport(WISP_THREAD_WRAPPER_TYPE, items);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new RuntimeException(e);
        }
    }

    private static final String THREAD_ID       = "threadId";
    private static final String THREAD_NAME     = "threadName";
    private static final String THREAD_STATE    = "threadState";
    private static final String BLOCKED_TIME    = "blockedTime";
    private static final String BLOCKED_COUNT   = "blockedCount";
    private static final String WAITED_TIME     = "waitedTime";
    private static final String WAITED_COUNT    = "waitedCount";
    private static final String LOCK_INFO       = "lockInfo";
    private static final String LOCK_NAME       = "lockName";
    private static final String LOCK_OWNER_ID   = "lockOwnerId";
    private static final String LOCK_OWNER_NAME = "lockOwnerName";
    private static final String STACK_TRACE     = "stackTrace";
    private static final String SUSPENDED       = "suspended";
    private static final String IN_NATIVE       = "inNative";
    private static final String DAEMON          = "daemon";
    private static final String PRIORITY        = "priority";
    private static final String LOCKED_MONITORS = "lockedMonitors";
    private static final String LOCKED_SYNCS    = "lockedSynchronizers";

    private static final CompositeType WISP_THREAD_WRAPPER_TYPE;

    static {
        try {
            WISP_THREAD_WRAPPER_TYPE = (CompositeType)
                    MappedMXBeanType.toOpenType(ThreadInfo.class);
        } catch (OpenDataException e) {
            // Should never reach here
            throw new RuntimeException(e);
        }
    }

    private static final long serialVersionUID = -2490192979220709669L;
}
