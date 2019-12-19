package com.alibaba.wisp.engine;

final public class WispCounter {

    private long switchCount = 0;

    private long waitTimeTotal = 0;

    private long runningTimeTotal = 0;

    private long completedTaskCount = 0;

    private long createTaskCount = 0;

    private long parkCount = 0;

    private long unparkCount = 0;

    private long unparkInterruptSelectorCount = 0;

    private long selectableIOCount = 0;

    private long timeOutCount = 0;

    private long eventLoopCount = 0;

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

    private long unparkFromJvmCount = 0;

    WispEngine engine;

    private WispCounter(WispEngine engine) {
        this.engine = engine;
    }

    public boolean getRunningState() {
        WispEngine e = engine;
        return e != null && e.isRunning();
    }

    void incrementSwitchCount() {
        switchCount++;
    }

    public long getSwitchCount() {
        return switchCount;
    }

    void incrementCompleteTaskCount() {
        completedTaskCount++;
    }

    void incrementRunningTimeTotal(long value) {
        runningTimeTotal += value;
    }

    public long getRunningTimeTotal() {
        return runningTimeTotal;
    }

    void incrementWaitTime(long value) {
        waitTimeTotal += value;
    }

    public long getWaitTimeTotal() {
        return waitTimeTotal;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    void incrementCreateTaskCount() {
        createTaskCount++;
    }

    public long getCreateTaskCount() {
        return createTaskCount;
    }

    void incrementParkCount() {
        parkCount++;
    }

    public long getParkCount() {
        return parkCount;
    }


    void incrementUnparkInterruptSelectorCount() {
        unparkInterruptSelectorCount++;
    }

    public long getUnparkInterruptSelectorCount() {
        return unparkInterruptSelectorCount;
    }

    void incrementSelectableIOCount() {
        selectableIOCount++;
    }

    public long getSelectableIOCount() {
        return selectableIOCount;
    }

    void incrementTimeOutCount() {
        timeOutCount++;
    }

    public long getTimeOutCount() {
        return timeOutCount;
    }

    void incrementEventLoopCount() {
        eventLoopCount++;
    }

    public long getEventLoopCount() {
        return eventLoopCount;
    }

    void incrementTotalEnqueueTime(long value) {
        totalEnqueueTime += value;
        enqueueCount++;
        if (value > maxEnqueueTime) {
            maxEnqueueTime = value;
        }
    }

    public long getTotalEnqueueTime() {
        return totalEnqueueTime;
    }

    public long getEnqueueCount() {
        return enqueueCount;
    }

    void incrementTotalExecutionTime(long value) {
        totalExecutionTime += value;
        executionCount++;
        if (value > maxExecutionTime) {
            maxExecutionTime = value;
        }
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    void incrementTotalWaitSocketIOTime(long value) {
        totalWaitSocketIOTime += value;
        waitSocketIOCount++;
        if (value > maxWaitSocketIOTime) {
            maxWaitSocketIOTime = value;
        }
    }

    public long getTotalWaitSocketIOTime() {
        return totalWaitSocketIOTime;
    }

    public long getWaitSocketIOCount() {
        return waitSocketIOCount;
    }

    void incrementTotalBlockingTime(long value) {
        totalBlockingTime += value;
        unparkCount++;
        if (value > maxBlockingTime) {
            maxBlockingTime = value;
        }
    }

    public long getTotalBlockingTime() {
        return totalBlockingTime;
    }

    public long getUnparkCount() {
        return unparkCount;
    }

    public long getTaskQueueLength() {
        WispEngine e = engine;
        return e != null ? e.getTaskQueueLength() : 0;
    }

    public long getRunningTaskCount() {
        WispEngine e = engine;
        return e != null ? e.getRunningTaskCount() : 0;
    }

    void incrementUnparkFromJvmCount() {
        unparkFromJvmCount++;
    }

    public long getUnparkFromJvmCount() {
        return unparkFromJvmCount;
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

    WispCounter() {}

    void assign(WispCounter counter) {
        createTaskCount = counter.createTaskCount;
        completedTaskCount = counter.completedTaskCount;
        totalEnqueueTime = counter.totalEnqueueTime;
        enqueueCount = counter.enqueueCount;
        maxEnqueueTime = counter.maxEnqueueTime;
        totalExecutionTime = counter.totalExecutionTime;
        executionCount = counter.executionCount;
        maxExecutionTime = counter.maxExecutionTime;
        totalBlockingTime = counter.totalBlockingTime;
        unparkCount = counter.unparkCount;
        maxBlockingTime = counter.maxBlockingTime;
        totalWaitSocketIOTime = counter.totalWaitSocketIOTime;
        waitSocketIOCount = counter.waitSocketIOCount;
        maxWaitSocketIOTime = counter.maxWaitSocketIOTime;
        switchCount = counter.switchCount;
        unparkFromJvmCount = counter.unparkFromJvmCount;
    }

    void resetMaxValue() {
        maxEnqueueTime = 0;
        maxExecutionTime = 0;
        maxWaitSocketIOTime = 0;
        maxBlockingTime = 0;
    }

    void cleanup() {
        engine = null;
    }

    static WispCounter create(WispEngine engine) {
        return new WispCounter(engine);
    }
}
