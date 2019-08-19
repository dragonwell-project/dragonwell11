package com.alibaba.wisp.engine;

final class WispCounter {

    private long switchCount = 0;

    private long waitTimeTotal = 0;

    private long runningTimeTotal = 0;

    private long completeTaskCount = 0;

    private long createTaskCount = 0;

    private long parkCount = 0;

    private long unparkCount = 0;

    private long unparkInterruptSelectorCount = 0;

    private long selectableIOCount = 0;

    private long timeOutCount = 0;

    private long eventLoopCount = 0;

    private long totalEnqueueTime = 0;

    private long enqueueCount = 0;

    private long totalExecutionTime = 0;

    private long executionCount = 0;

    private long totalWaitSocketIOTime = 0;

    private long waitSocketIOCount = 0;

    private long totalBlockingTime = 0;

    private long unparkFromJvmCount = 0;

    WispEngine engine;

    private WispCounter(WispEngine engine) {
        this.engine = engine;
    }

    boolean getRunningState() {
        return engine.isRunning();
    }

    void incrementSwitchCount() {
        switchCount++;
    }

    long getSwitchCount() {
        return switchCount;
    }

    void incrementCompleteTaskCount() {
        completeTaskCount++;
    }

    void incrementRunningTimeTotal(long value) {
        runningTimeTotal += value;
    }

    long getRunningTimeTotal() {
        return runningTimeTotal;
    }

    void incrementWaitTime(long value) {
        waitTimeTotal += value;
    }

    long getWaitTimeTotal() {
        return waitTimeTotal;
    }

    long getCompleteTaskCount() {
        return completeTaskCount;
    }

    void incrementCreateTaskCount() {
        createTaskCount++;
    }

    long getCreateTaskCount() {
        return createTaskCount;
    }

    void incrementParkCount() {
        parkCount++;
    }

    long getParkCount() {
        return parkCount;
    }


    void incrementUnparkInterruptSelectorCount() {
        unparkInterruptSelectorCount++;
    }

    long getUnparkInterruptSelectorCount() {
        return unparkInterruptSelectorCount;
    }

    void incrementSelectableIOCount() {
        selectableIOCount++;
    }

    long getSelectableIOCount() {
        return selectableIOCount;
    }

    void incrementTimeOutCount() {
        timeOutCount++;
    }

    long getTimeOutCount() {
        return timeOutCount;
    }

    void incrementEventLoopCount() {
        eventLoopCount++;
    }

    long getEventLoopCount() {
        return eventLoopCount;
    }

    void incrementTotalEnqueueTime(long value) {
        totalEnqueueTime += value;
        enqueueCount++;
    }

    long getTotalEnqueueTime() {
        return totalEnqueueTime;
    }

    long getEnqueueCount() {
        return enqueueCount;
    }

    void incrementTotalExecutionTime(long value) {
        totalExecutionTime += value;
        executionCount++;
    }

    long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    long getExecutionCount() {
        return executionCount;
    }

    void incrementTotalWaitSocketIOTime(long value) {
        totalWaitSocketIOTime += value;
        waitSocketIOCount++;
    }

    long getTotalWaitSocketIOTime() {
        return totalWaitSocketIOTime;
    }

    long getWaitSocketIOCount() {
        return waitSocketIOCount;
    }

    void incrementTotalBlockingTime(long value) {
        totalBlockingTime += value;
        unparkCount++;
    }

    long getTotalBlockingTime() {
        return totalBlockingTime;
    }

    long getUnparkCount() {
        return unparkCount;
    }

    public long getQueueLength() {
        return engine.getTaskQueueLength();
    }

    long getNumberOfRunningTasks() {
        return engine.getNumberOfrunningTaskCount();
    }

    void incrementUnparkFromJvmCount() {
        unparkFromJvmCount++;
    }

    long getUnparkFromJvmCount() {
        return unparkFromJvmCount;
    }

    WispCounter() {}

    void assign(WispCounter counter) {
        createTaskCount =  counter.createTaskCount;
        completeTaskCount = counter.completeTaskCount;
        totalEnqueueTime = counter.totalEnqueueTime;
        enqueueCount = counter.enqueueCount;
        totalExecutionTime = counter.totalExecutionTime;
        executionCount = counter.executionCount;
        totalBlockingTime = counter.totalBlockingTime;
        unparkCount = counter.unparkCount;
        totalWaitSocketIOTime = counter.totalWaitSocketIOTime;
        waitSocketIOCount = counter.waitSocketIOCount;
        switchCount = counter.switchCount;
        unparkFromJvmCount = counter.unparkFromJvmCount;
    }

    public static WispCounter create(WispEngine engine) {
        return new WispCounter(engine);
    }
}
