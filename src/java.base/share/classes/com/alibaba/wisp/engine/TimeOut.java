package com.alibaba.wisp.engine;

import java.util.Arrays;

/**
 * Represents {@link WispTask} related time out
 */
public class TimeOut {
    final WispTask task;
    long deadlineNano;
    boolean canceled = false;
    /**
     * its position in the heapArray, -1 indicates that TimeOut has been deleted
     */
    private final boolean fromJvm;
    private int queueIdx;
    private TimerManager manager;

    /**
     * @param task         related {@link WispTask}
     * @param deadlineNano wake up related {@link WispTask} at {@code deadline} if not canceled
     */
    public TimeOut(WispTask task, long deadlineNano, boolean fromJvm) {
        this.task = task;
        this.deadlineNano = deadlineNano;
        this.fromJvm = fromJvm;
    }

    /**
     * @return {@code true} if and only if associated timer is expired.
     */
    public boolean expired() {
        return !canceled && System.nanoTime() >= deadlineNano;
    }

    /**
     * unpark the blocked task
     */
    void doUnpark() {
        if (fromJvm) {
            task.unpark();
        } else {
            task.jdkUnpark();
        }
    }

    /**
     * We use minimum heap algorithm to get the minimum deadline of all timers,
     * also keep every TimeOut's queueIdx(its position in heap) so that we can easily
     * remove it.
     */
    static class TimerManager {
        Queue queue = new Queue();

        void addTimer(TimeOut timeOut) {
            timeOut.deadlineNano = overflowFree(timeOut.deadlineNano, queue.peek());
            timeOut.manager = this;
            queue.offer(timeOut);
        }

        void cancelTimer(TimeOut timeOut) {
            if (timeOut.manager == this && timeOut.queueIdx != -1) {
                queue.remove(timeOut);
            }
        }

        /**
         * Dispatch timeout events and return the timeout interval for next
         * first timeout task
         *
         * @return -1: infinitely waiting, > 0 wait nanos
         */
        long processTimeoutEventsAndGetWaitNanos() {
            long nanos = -1;
            TimeOut timeOut;
            if (queue.size != 0) {
                long now = System.nanoTime();
                while ((timeOut = queue.peek()) != null) {
                    if (timeOut.canceled) {
                        queue.poll();
                    } else if (timeOut.deadlineNano <= now) {
                        queue.poll();
                        timeOut.doUnpark();
                    } else {
                        nanos = timeOut.deadlineNano - now;
                        break;
                    }
                }
            }
            return nanos;
        }

        class Queue {
            private static final int INITIAL_CAPACITY = 16;
            private TimeOut[] queue = new TimeOut[INITIAL_CAPACITY];
            private int size = 0;

            /**
             * Inserts TimeOut x at position k, maintaining heap invariant by
             * promoting x up the tree until it is greater than or equal to
             * its parent, or is the root.
             *
             * @param k       the position to fill
             * @param timeOut the TimeOut to insert
             */
            private void siftUp(int k, TimeOut timeOut) {
                while (k > 0) {
                    int parent = (k - 1) >>> 1;
                    TimeOut e = queue[parent];
                    if (timeOut.deadlineNano >= e.deadlineNano) {
                        break;
                    }
                    queue[k] = e;
                    e.queueIdx = k;
                    k = parent;
                }
                queue[k] = timeOut;
                timeOut.queueIdx = k;
            }

            /**
             * Inserts item x at position k, maintaining heap invariant by
             * demoting x down the tree repeatedly until it is less than or
             * equal to its children or is a leaf.
             *
             * @param k       the position to fill
             * @param timeOut the item to insert
             */
            private void siftDown(int k, TimeOut timeOut) {
                int half = size >>> 1;
                while (k < half) {
                    int child = (k << 1) + 1;
                    TimeOut c = queue[child];
                    int right = child + 1;
                    if (right < size && c.deadlineNano > queue[right].deadlineNano) {
                        c = queue[child = right];
                    }
                    if (timeOut.deadlineNano <= c.deadlineNano) {
                        break;
                    }
                    queue[k] = c;
                    c.queueIdx = k;
                    k = child;
                }
                queue[k] = timeOut;
                timeOut.queueIdx = k;
            }


            public boolean remove(TimeOut timeOut) {
                int i = timeOut.queueIdx;
                if (i == -1) {
                    //this timeOut has been deleted.
                    return false;
                }

                queue[i].queueIdx = -1;
                int s = --size;
                TimeOut replacement = queue[s];
                queue[s] = null;
                if (s != i) {
                    siftDown(i, replacement);
                    if (queue[i] == replacement) {
                        siftUp(i, replacement);
                    }
                }
                return true;
            }

            public int size() {
                return size;
            }

            public boolean offer(TimeOut timeOut) {
                int i = size++;
                if (i >= queue.length) {
                    queue = Arrays.copyOf(queue, queue.length * 2);
                }
                if (i == 0) {
                    queue[0] = timeOut;
                    timeOut.queueIdx = 0;
                } else {
                    siftUp(i, timeOut);
                }
                return true;
            }

            public TimeOut poll() {
                if (size == 0) {
                    return null;
                }
                TimeOut f = queue[0];
                int s = --size;
                TimeOut x = queue[s];
                queue[s] = null;
                if (s != 0) {
                    siftDown(0, x);
                }
                f.queueIdx = -1;
                return f;
            }

            public TimeOut peek() {
                return queue[0];
            }
        }
    }

    private static long overflowFree(long deadlineNano, TimeOut head) {
        if (deadlineNano < Long.MIN_VALUE / 2) { // deadlineNano regarded as negative overflow
            deadlineNano = Long.MAX_VALUE;
        }
        if (head != null && head.deadlineNano < 0 && deadlineNano > 0 && deadlineNano - head.deadlineNano < 0) {
            deadlineNano = Long.MAX_VALUE + head.deadlineNano;
            // then deadlineNano - head.deadlineNano = Long.MAX_VALUE > 0
            // i.e. deadlineNano > head.deadlineNano
        }
        return deadlineNano;
    }
}

