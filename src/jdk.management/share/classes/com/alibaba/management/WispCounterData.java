/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.alibaba.management;

import com.alibaba.wisp.engine.WispCounter;
import javax.management.ConstructorParameters;

public class WispCounterData {
    private long completedTaskCount = 0;

    private long unparkCount = 0;

    private long totalEnqueueTime = 0;

    private long maxEnqueueTime = 0;

    private long enqueueCount = 0;

    private long totalExecutionTime = 0;

    private long maxExecutionTime = 0;

    private long executionCount = 0;

    private long totalWaitSocketIOTime = 0;

    private long maxWaitSocketIOTime = 0;

    private long waitSocketIOCount = 0;

    private long totalBlockingTime = 0;

    private long maxBlockingTime = 0;

    private long runningTaskCount = 0;

    private long taskQueueLength = 0;

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public long getTotalEnqueueTime() {
        return totalEnqueueTime;
    }

    public long getEnqueueCount() {
        return enqueueCount;
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public long getTotalWaitSocketIOTime() {
        return totalWaitSocketIOTime;
    }

    public long getWaitSocketIOCount() {
        return waitSocketIOCount;
    }

    public long getTotalBlockingTime() {
        return totalBlockingTime;
    }

    public long getUnparkCount() {
        return unparkCount;
    }

    public long getMaxEnqueueTime() {
        return maxEnqueueTime;
    }

    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }

    public long getMaxWaitSocketIOTime() {
        return maxWaitSocketIOTime;
    }

    public long getMaxBlockingTime() {
        return maxBlockingTime;
    }

    public long getTaskQueueLength() {
        return taskQueueLength;
    }

    public long getRunningTaskCount() {
        return runningTaskCount;
    }

    @ConstructorParameters({"completedTaskCount", "totalEnqueueTime", "maxEnqueueTime", "enqueueCount",
                            "totalExecutionTime", "maxExecutionTime", "executionCount",
                            "totalWaitSocketIOTime", "maxWaitSocketIOTime", "waitSocketIOCount",
                            "totalBlockingTime", "maxBlockingTime", "unparkCount",
                            "runningTaskCount", "taskQueueLength"})
    public WispCounterData(long completedTaskCount, long totalEnqueueTime, long maxEnqueueTime, long enqueueCount,
                       long totalExecutionTime, long maxExecutionTime, long executionCount,
                       long totalWaitSocketIOTime, long maxWaitSocketIOTime, long waitSocketIOCount,
                       long totalBlockingTime, long maxBlockingTime, long unparkCount,
                       long runningTaskCount, long taskQueueLength) {
        this.completedTaskCount = completedTaskCount;
        this.totalEnqueueTime = totalEnqueueTime;
        this.maxEnqueueTime = maxEnqueueTime;
        this.enqueueCount = enqueueCount;
        this.totalExecutionTime = totalExecutionTime;
        this.maxExecutionTime = maxExecutionTime;
        this.executionCount = executionCount;
        this.totalWaitSocketIOTime = totalWaitSocketIOTime;
        this.maxWaitSocketIOTime = maxWaitSocketIOTime;
        this.waitSocketIOCount = waitSocketIOCount;
        this.totalBlockingTime = totalBlockingTime;
        this.maxBlockingTime = maxBlockingTime;
        this.unparkCount = unparkCount;
        this.runningTaskCount = runningTaskCount;
        this.taskQueueLength = taskQueueLength;
    }

    public WispCounterData(WispCounter counter) {
        completedTaskCount = counter.getCompletedTaskCount();
        totalEnqueueTime = counter.getTotalEnqueueTime();
        enqueueCount = counter.getEnqueueCount();
        maxEnqueueTime = counter.getMaxEnqueueTime();
        totalExecutionTime = counter.getTotalExecutionTime();
        executionCount = counter.getExecutionCount();
        maxExecutionTime = counter.getMaxExecutionTime();
        totalBlockingTime = counter.getTotalBlockingTime();
        unparkCount = counter.getUnparkCount();
        maxBlockingTime = counter.getMaxBlockingTime();
        totalWaitSocketIOTime = counter.getTotalWaitSocketIOTime();
        waitSocketIOCount = counter.getWaitSocketIOCount();
        maxWaitSocketIOTime = counter.getMaxWaitSocketIOTime();
        runningTaskCount = counter.getRunningTaskCount();
        taskQueueLength = counter.getTaskQueueLength();
    }
}
