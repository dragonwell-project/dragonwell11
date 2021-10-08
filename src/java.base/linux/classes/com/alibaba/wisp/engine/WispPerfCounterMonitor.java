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

package com.alibaba.wisp.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

enum WispPerfCounterMonitor {
    INSTANCE;

    private boolean fileHandleEnable = false;
    private Map<Long, WispPerfCounter> managedEngineCounters;
    private PrintStream printStream;
    private SimpleDateFormat localDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    WispPerfCounterMonitor() {
        if (WispConfiguration.WISP_PROFILE) {
            managedEngineCounters = new ConcurrentHashMap<>(100);
        }
        if (WispConfiguration.WISP_PROFILE_LOG_ENABLED) {
            String logPath = WispConfiguration.WISP_PROFILE_LOG_PATH;
            try {
                printStream = new PrintStream(new File(logPath == null?
                        "wisplog.log" : logPath + File.separator + "wisplog%g.log"));
                fileHandleEnable = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void startDaemon() {
        Thread thread = new Thread(WispEngine.daemonThreadGroup, this::perfCounterLoop, "Wisp-Monitor");
        thread.setDaemon(true);
        thread.start();
    }

    void register(WispCounter counter) {
        if (WispConfiguration.WISP_PROFILE) {
            WispPerfCounter wispPerfCounter = new WispPerfCounter(counter);
            managedEngineCounters.put(counter.carrier.getId(), wispPerfCounter);
        }
    }

    void deRegister(WispCounter counter) {
        if (WispConfiguration.WISP_PROFILE) {
            managedEngineCounters.remove(counter.carrier.getId());
        }
    }

    WispCounter getWispCounter(long id) {
        WispPerfCounter perfCounter = WispEngine.runInCritical(
                () -> managedEngineCounters.get(id));
        if (perfCounter == null) {
            return null;
        }
        perfCounter.storeCurrentWispCounter();
        return perfCounter.prevCounterValue;
    }

    private void perfCounterLoop() {
        while (true) {
            try {
                Thread.sleep((long) WispConfiguration.WISP_PROFILE_LOG_INTERVAL_MS);
            } catch (InterruptedException e) {
                // pass
            }
            dumpCounter();
        }
    }

    private void appendLogString(StringBuilder strb, String dateTime, String item, int workerID, long data) {
        strb.append(dateTime)
                .append("\t").append(item).append("\t\t")
                .append("worker").append(workerID).append("\t\t")
                .append(data).append("\n");
    }

    private void dumpCounter() {
        if (!fileHandleEnable) {
            return;
        }

        StringBuilder strb = new StringBuilder();
        long currentTime = System.currentTimeMillis();
        String dateTime = localDateFormat.format(new Date(currentTime));
        int worker = 0;
        /* dump Wisp monitor information */
        WispPerfCounter perfCounter;
        for (Entry<Long, WispPerfCounter> entry : managedEngineCounters.entrySet()) {
            perfCounter = entry.getValue();
            appendLogString(strb, dateTime, "completedTaskCount", worker, perfCounter.getCompletedTaskCount());
            appendLogString(strb, dateTime, "unparkFromJvmCount", worker, perfCounter.getUnparkFromJvmCount());
            appendLogString(strb, dateTime, "averageEnqueueTime", worker, perfCounter.getAverageEnqueueTime());
            appendLogString(strb, dateTime, "averageExecutionTime", worker, perfCounter.getAverageExecutionTime());
            appendLogString(strb, dateTime, "averageWaitSocketIOTime", worker, perfCounter.getAverageWaitSocketIOTime());
            appendLogString(strb, dateTime, "averageBlockingTime", worker, perfCounter.getAverageBlockingTime());
            perfCounter.storeCurrentWispCounter();
            worker++;
        }
        printStream.print(strb.toString());
        printStream.println();
        printStream.flush();
    }

    private class WispPerfCounter {
        WispCounter counter;

        WispCounter prevCounterValue;

        long getCompletedTaskCount() {
            return counter.getCompletedTaskCount() - prevCounterValue.getCompletedTaskCount();
        }

        long getUnparkFromJvmCount() {
            return counter.getUnparkFromJvmCount() - prevCounterValue.getUnparkFromJvmCount();
        }

        long getAverageTime(Function<WispCounter, Long> timeFunc, Function<WispCounter, Long> countFunc) {
            long count = countFunc.apply(counter) - countFunc.apply(prevCounterValue);
            if (count == 0) {
                return 0;
            }
            long totalNanos = timeFunc.apply(counter) - timeFunc.apply(prevCounterValue);
            return totalNanos / count;
        }

        long getAverageEnqueueTime() {
            return getAverageTime(WispCounter::getTotalEnqueueTime, WispCounter::getEnqueueCount);
        }

        long getAverageExecutionTime() {
            return getAverageTime(WispCounter::getTotalExecutionTime, WispCounter::getExecutionCount);
        }

        long getAverageWaitSocketIOTime() {
            return getAverageTime(WispCounter::getTotalWaitSocketIOTime, WispCounter::getWaitSocketIOCount);
        }

        long getAverageBlockingTime() {
            return getAverageTime(WispCounter::getTotalBlockingTime, WispCounter::getUnparkCount);
        }

        void storeCurrentWispCounter() {
            if (counter == null) {
                return;
            }
            prevCounterValue.assign(counter);
            counter.resetMaxValue();
        }

        WispPerfCounter(WispCounter counter) {
            this.counter = counter;
            this.prevCounterValue = new WispCounter();
            this.prevCounterValue.assign(counter);
        }
    }
}
