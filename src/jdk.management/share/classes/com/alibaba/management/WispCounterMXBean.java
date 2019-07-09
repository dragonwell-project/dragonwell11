package com.alibaba.management;

import com.alibaba.wisp.engine.WispCounter;

import java.lang.management.PlatformManagedObject;
import java.util.List;

public interface WispCounterMXBean extends PlatformManagedObject {

    /**
     * @return list of managed wisp worker running state
     */
    List<Boolean> getRunningStates();

    /**
     * @return list of managed wisp worker switch count
     */
    List<Long> getSwitchCount();

    /**
     * @return list of managed wisp worker wait time total, unit ns
     */
    List<Long> getWaitTimeTotal();

    /**
     * @return list of managed wisp worker running time total, unit ns
     */
    List<Long> getRunningTimeTotal();

    /**
     * @return list of managed wisp worker complete task count
     */
    List<Long> getCompleteTaskCount();

    /**
     * @return list of managed wisp worker create task count
     */
    List<Long> getCreateTaskCount();

    /**
     * @return list of managed wisp worker park count
     */
    List<Long> getParkCount();

    /**
     * @return list of managed wisp worker unpark count
     */
    List<Long> getUnparkCount();

    /**
     * @return list of managed wisp worker lazy unpark count
     */
    List<Long> getLazyUnparkCount();

    /**
     * @return list of managed wisp worker unpark interrupt selector count
     */
    List<Long> getUnparkInterruptSelectorCount();

    /**
     * @return list of managed wisp worker do IO count
     */
    List<Long> getSelectableIOCount();

    /**
     * @return list of managed wisp worker timeout count
     */
    List<Long> getTimeOutCount();

    /**
     * @return list of managed wisp worker do event loop count
     */
    List<Long> getEventLoopCount();

    /**
     * @return list of managed wisp worker task queue length
     */
    List<Long> getQueueLength();

    /**
     * @return list of number of running tasks from managed wisp workers
     */
    List<Long> getNumberOfRunningTasks();

    /**
     * @return list of total blocking time in nanos from managed wisp workers
     */
    List<Long> getTotalBlockingTime();

    /**
     * @return list of total execution time in nanos from managed wisp workers
     */
    List<Long> getTotalExecutionTime();

    /**
     * @return list of execution count from managed wisp workers
     */
    List<Long> getExecutionCount();

    /**
     * @return list of total enqueue time in nanos from managed wisp workers
     */
    List<Long> getTotalEnqueueTime();

    /**
     * @return list of enqueue count from managed wisp workers
     */
    List<Long> getEnqueueCount();

    /**
     * @return list of total wait socket io time in nanos from managed wisp workers
     */
    List<Long> getTotalWaitSocketIOTime();

    /**
     * @return list of wait socket io event count from managed wisp workers
     */
    List<Long> getWaitSocketIOCount();

    /**
     * @param id WispEngine id
     * @return WispCounterData
     */
    WispCounterData getWispCounterData(long id);
}
