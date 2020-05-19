package com.alibaba.rcm.internal;


import com.alibaba.rcm.ResourceContainer;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Provide unsafe access to internal ResourceContainer management,
 * such as predicate whether inheriting current thread or not, set
 * current ResourceContainer's inheritance callback.
 * <p>
 * Extra attention is required when using this class.
 */
public final class RCMUnsafe {
    /**
     * Set a runtime predicate to control thread's {@link  ResourceContainer}
     * inheritance behavior, here is an example for inheriting parent thread's
     * context resource container to current child thread only if a thread is
     * named "tenantWorker".
     * {
     *      ResourceContainer container = ResourceContainerFactory.createContainer();
     *      RCMUnsafe.setResourceContainerInheritancePredicate(container,
     *      (thread) -> thread.getName().equals("tenantWorker");
     * }
     *
     * @param container which container to set predicate on
     * @param predicate which predicate would be used at runtime
     */
    public static void setResourceContainerInheritancePredicate(ResourceContainer container, Predicate<Thread> predicate) {
        ((AbstractResourceContainer) container).setUnsafeThreadInheritancePredicate(predicate);
    }

    /**
     * Stop all threads that inherited from {@parm resourceContainer}.
     * @param resourceContainer specified container
     */
    public static void killThreads(ResourceContainer resourceContainer) {
        assert resourceContainer instanceof AbstractResourceContainer;
        Objects.requireNonNull(resourceContainer);
        ((AbstractResourceContainer) resourceContainer).killThreads();
    }
}