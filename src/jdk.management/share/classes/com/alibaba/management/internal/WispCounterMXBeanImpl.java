package com.alibaba.management.internal;

import com.alibaba.management.WispCounterMXBean;
import com.alibaba.management.WispCounterData;
import com.alibaba.wisp.engine.WispCounter;
import com.alibaba.wisp.engine.WispEngine;
import sun.management.Util;

import javax.management.ObjectName;
import java.util.*;
import java.util.function.Function;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation class for WispCounterMXBean.
 */
public class WispCounterMXBeanImpl implements com.alibaba.management.WispCounterMXBean {

    private final static String WISP_COUNTER_MXBEAN_NAME = "com.alibaba.management:type=WispCounter";

    @Override
    public List<Boolean> getRunningStates() {
        return aggregate(WispCounter::getRunningState);
    }

    @Override
    public List<Long> getSwitchCount() {
        return aggregate(WispCounter::getSwitchCount);
    }

    @Override
    public List<Long> getWaitTimeTotal() {
        return aggregate(WispCounter::getWaitTimeTotal);
    }

    @Override
    public List<Long> getRunningTimeTotal() {
        return aggregate(WispCounter::getRunningTimeTotal);
    }

    @Override
    public List<Long> getCompleteTaskCount() {
        return aggregate(WispCounter::getCompletedTaskCount);
    }

    @Override
    public List<Long> getCreateTaskCount() {
        return aggregate(WispCounter::getCreateTaskCount);
    }

    @Override
    public List<Long> getParkCount() {
        return aggregate(WispCounter::getParkCount);
    }

    @Override
    public List<Long> getUnparkCount() {
        return aggregate(WispCounter::getUnparkCount);
    }

    @Override
    public List<Long> getLazyUnparkCount() {
        return aggregate(w -> 0L);
    }

    @Override
    public List<Long> getUnparkInterruptSelectorCount() {
        return aggregate(WispCounter::getUnparkInterruptSelectorCount);
    }

    @Override
    public List<Long> getSelectableIOCount() {
        return aggregate(WispCounter::getSelectableIOCount);
    }

    @Override
    public List<Long> getTimeOutCount() {
        return aggregate(WispCounter::getTimeOutCount);
    }

    @Override
    public List<Long> getEventLoopCount() {
        return aggregate(WispCounter::getEventLoopCount);
    }

    @Override
    public List<Long> getQueueLength() {
        return aggregate(WispCounter::getTaskQueueLength);
    }

    @Override
    public List<Long> getNumberOfRunningTasks() {
        return aggregate(WispCounter::getRunningTaskCount);
    }

    @Override
    public List<Long> getTotalEnqueueTime() {
        return aggregate(WispCounter::getTotalEnqueueTime);
    }

    @Override
    public List<Long> getEnqueueCount() {
        return aggregate(WispCounter::getEnqueueCount);
    }

    @Override
    public List<Long> getTotalExecutionTime() {
        return aggregate(WispCounter::getTotalExecutionTime);
    }

    @Override
    public List<Long> getExecutionCount() {
        return aggregate(WispCounter::getExecutionCount);
    }

    @Override
    public List<Long> getTotalWaitSocketIOTime() {
        return aggregate(WispCounter::getTotalWaitSocketIOTime);
    }

    @Override
    public List<Long> getWaitSocketIOCount() {
        return aggregate(WispCounter::getWaitSocketIOCount);
    }

    @Override
    public List<Long> getTotalBlockingTime() {
        return aggregate(WispCounter::getTotalBlockingTime);
    }

    /**
     * @param id WispEngine id
     * @return WispCounterData
     */
    @Override
    public WispCounterData getWispCounterData(long id) {
        WispCounter counter = WispEngine.getWispCounter(id);
        return new WispCounterData(counter);
    }

    private <T> List<T> aggregate(Function<WispCounter, T> getter) {
        if (!WispEngine.transparentWispSwitch()) {
            return null;
        }
        List<T> result = new ArrayList<>(WispEngine.getManagedEngineCounters().size());
        for (Entry<Long, WispCounter> entry : WispEngine.getManagedEngineCounters().entrySet()) {
            result.add(getter.apply(entry.getValue()));
        }
        return result;
    }

    @Override
    public ObjectName getObjectName() {
        return Util.newObjectName(WISP_COUNTER_MXBEAN_NAME);
    }
}
