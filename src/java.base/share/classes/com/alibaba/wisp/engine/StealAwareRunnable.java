package com.alibaba.wisp.engine;


/**
 * An runnable that is aware of work stealing.
 */
public interface StealAwareRunnable extends Runnable {

    /**
     * @return if that runnable could be stolen
     */
    default boolean isStealEnable() {
        return true;
    }

    /**
     * Set this runnable's {@link #isStealEnable} to given value
     */
    default void setStealEnable(boolean b) {
    }
}
