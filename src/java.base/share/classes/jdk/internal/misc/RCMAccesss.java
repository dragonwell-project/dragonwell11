package jdk.internal.misc;

import com.alibaba.rcm.ResourceContainer;

import java.util.function.Predicate;

public interface RCMAccesss {
    /**
     * Get container's thread inheritance predicate.
     * @param container current child container in parent thread's context
     * @return parent thread's inheritance predicate
     */
    public Predicate<Thread> getResourceContainerInheritancePredicate(ResourceContainer container);
}
