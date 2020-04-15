package com.alibaba.rcm.internal;

import com.alibaba.rcm.Constraint;
import com.alibaba.rcm.ResourceContainer;
import jdk.internal.misc.VM;
import jdk.internal.misc.SharedSecrets;

import java.util.Collections;

/**
 * A skeletal implementation of {@link ResourceContainer} that practices
 * the attach/detach paradigm described in {@link ResourceContainer#run(Runnable)}.
 * <p>
 * Each {@code ResourceContainer} implementation must inherit from this class.
 *
 * @see ResourceContainer#run(Runnable)
 */

public abstract class AbstractResourceContainer implements ResourceContainer {

    protected final static AbstractResourceContainer ROOT = new RootContainer();

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
    }
}
