package com.alibaba.rcm;

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import com.alibaba.rcm.ResourceContainerMonitor;
import com.alibaba.rcm.ResourceType;
import jdk.internal.misc.RCMAccesss;
import jdk.internal.misc.VM;
import jdk.internal.misc.SharedSecrets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A skeletal implementation of {@link ResourceContainer} that practices
 * the attach/detach paradigm described in {@link ResourceContainer#run(Runnable)}.
 * <p>
 * Each {@code ResourceContainer} implementation must inherit from this class.
 *
 * @see ResourceContainer#run(Runnable)
 */

public abstract class AbstractResourceContainer implements ResourceContainer {

    static {
        setRCMAccess();
    }

    private static final Predicate<Thread> DEFAULT_PREDICATE = new Predicate<Thread>() {
        @Override
        public boolean test(Thread thread) {
            return true;
        }
    };

    private static void setRCMAccess() {
        SharedSecrets.setRCMAccesss(new RCMAccesss() {

            @Override
            public Predicate<Thread> getResourceContainerInheritancePredicate(ResourceContainer container) {
                return ((AbstractResourceContainer) container).threadInherited;
            }
        });
    }

    protected final static AbstractResourceContainer ROOT = new RootContainer();
    private Predicate<Thread> threadInherited = DEFAULT_PREDICATE;
    final long id;

    protected AbstractResourceContainer() {
        id = ResourceContainerMonitor.register(this);
    }


    public static AbstractResourceContainer root() {
        return ROOT;
    }

    public static AbstractResourceContainer current() {
        if (!VM.isBooted()) {
            // JLA will be available only after full VM bootstrap.
            // before that stage, we assume VM is running in ROOT container.
            return ROOT;
        }
        return SharedSecrets.getJavaLangAccess().getResourceContainer(Thread.currentThread());
    }

    public abstract List<Long> getActiveContainerThreadIds();

    public abstract Long getConsumedAmount(ResourceType resourceType);

    public abstract Long getResourceLimitReachedCount(ResourceType resourceType);

    @Override
    public void run(Runnable command) {
        if (getState() != State.RUNNING) {
            throw new IllegalStateException("container not running");
        }
        ResourceContainer container = current();
        if (container == this) {
            command.run();
        } else {
            if (container != ROOT) {
                throw new IllegalStateException("must be in root container " +
                        "before running into non-root container.");
            }
            attach();
            try {
                command.run();
            } finally {
                detach();
            }
        }
    }


    @Override
    public Long getId() {
        return id;
    }

    /**
     * Attach to this resource container.
     * Ensure {@link ResourceContainer#current()} as a root container
     * before calling this method.
     * <p>
     * The implementation class must call {@code super.attach()} to coordinate
     * with {@link ResourceContainer#current()}
     */
    protected void attach() {
        SharedSecrets.getJavaLangAccess().setResourceContainer(Thread.currentThread(), this);
    }

    /**
     * Detach from this resource container and return to root container.
     * <p>
     * The implementation class must call {@code super.detach()} to coordinate
     * with {@link ResourceContainer#current()}
     */
    protected void detach() {
        SharedSecrets.getJavaLangAccess().setResourceContainer(Thread.currentThread(), root());
    }

    void setUnsafeThreadInheritancePredicate(Predicate<Thread> predicate) {
        this.threadInherited = predicate;
    }

    protected void killThreads() {
        throw new UnsupportedOperationException("should not reach here");
    }


    private static class RootContainer extends AbstractResourceContainer {
        @Override
        public void run(Runnable command) {
            AbstractResourceContainer container = current();
            if (container == ROOT) {
                command.run();
                return;
            }
            container.detach();
            try {
                command.run();
            } finally {
                container.attach();
            }
        }

        @Override
        public ResourceContainer.State getState() {
            return ResourceContainer.State.RUNNING;
        }

        @Override
        protected void attach() {
            throw new UnsupportedOperationException("should not reach here");
        }

        @Override
        protected void detach() {
            throw new UnsupportedOperationException("should not reach here");
        }

        @Override
        public void updateConstraint(Constraint constraint) {
            throw new UnsupportedOperationException("updateConstraint() is not supported by root container");
        }

        @Override
        public Iterable<Constraint> getConstraints() {
            return Collections.emptyList();
        }

        @Override
        public void destroy() {
            throw new UnsupportedOperationException("destroy() is not supported by root container");
        }

        @Override
        public List<Long> getActiveContainerThreadIds() {
            ThreadGroup group = Thread.currentThread().getThreadGroup();
            while (group.getParent() != null) {
                group = group.getParent();
            }
            int count = group.activeCount();
            Thread[] threads;
            do {
                threads = new Thread[count + (count / 2) + 1]; //slightly grow the array size
                count = group.enumerate(threads, true);
                //return value of enumerate() must be strictly less than the array size according to javadoc
            } while (count == threads.length);

            final List<Long> result = new ArrayList<>(count);
            for (int i = 0; i < count; ++i) {
                if (SharedSecrets.getJavaLangAccess().getResourceContainer(threads[i]) == ROOT) {
                    result.add(threads[i].getId());
                }
            }
            return result;
        }

        @Override
        public Long getConsumedAmount(ResourceType resourceType) {
            return 0L;
        }

        @Override
        public Long getResourceLimitReachedCount(ResourceType resourceType) {
            return 0L;
        }

    }
}
