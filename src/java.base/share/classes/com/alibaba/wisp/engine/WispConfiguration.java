package com.alibaba.wisp.engine;

import sun.security.action.GetPropertyAction;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class WispConfiguration {
    private static final String DELIMITER = ";";

    static final boolean TRANSPARENT_WISP_SWITCH;
    static final boolean ENABLE_THREAD_AS_WISP;
    static final boolean ALL_THREAD_AS_WISP;
    static final boolean USE_THREAD_POOL_LIMIT;

    static final int STACK_SIZE;
    static final boolean PARK_ONE_MS_AT_LEAST;
    // Limit number of submitted tasks under threshold
    static final int EXTERNAL_SUBMIT_THRESHOLD;
    static final boolean GLOBAL_POLLER;
    static final int WISP_VERSION;
    static final int WORKER_COUNT;
    static final boolean ENABLE_HANDOFF;
    static final WispSysmon.Policy HANDOFF_POLICY;
    static final int SYSMON_TICK_US;
    static final int MIN_PARK_NANOS;

    static final int SYSMON_CARRIER_GROW_TICK_US;
    // monitor
    static final boolean WISP_PROFILE;
    static final boolean WISP_PROFILE_LOG_ENABLED;
    static final int WISP_PROFILE_LOG_INTERVAL_MS;
    static final String WISP_PROFILE_LOG_PATH;

    // wisp2
    static final boolean WISP_HIGH_PRECISION_TIMER;
    static final boolean WISP_USE_STEAL_LOCK;
    static final int WISP_ENGINE_TASK_CACHE_SIZE;
    static final int WISP_SCHEDULE_STEAL_RETRY;
    static final int WISP_SCHEDULE_PUSH_RETRY;
    static final int WISP_SCHEDULE_HELP_STEAL_RETRY;
    static final Wisp2Scheduler.SchedulingPolicy SCHEDULING_POLICY;
    static final boolean USE_DIRECT_SELECTOR_WAKEUP;
    static final boolean CARRIER_AS_POLLER;
    static final boolean MONOLITHIC_POLL;
    static final boolean CARRIER_GROW;

    // io
    static final boolean WISP_ENABLE_SOCKET_LOCK;

    private static List<String> PROXY_SELECTOR_STACK_LIST;
    private static List<String> SLEEP_SELECTOR_STACK_LIST;
    private static List<String> PUT_TO_CURRENT_STACK_LIST;
    private static List<String> PUT_TO_MANAGED_THREAD_STACK_LIST;
    private static List<String> PUT_STACK_BLACKLIST;
    private static List<String> THREAD_AS_WISP_BLACKLIST;


    static {
        Properties p = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Properties>() {
                    public Properties run() {
                        return System.getProperties();
                    }
                }
        );

        TRANSPARENT_WISP_SWITCH = p.containsKey("com.alibaba.wisp.transparentWispSwitch") ?
                parseBooleanParameter(p, "com.alibaba.wisp.transparentWispSwitch", false) :
                parseBooleanParameter(p, "com.alibaba.transparentAsync", false);
        ENABLE_THREAD_AS_WISP = p.containsKey("com.alibaba.wisp.enableThreadAsWisp") ?
                parseBooleanParameter(p, "com.alibaba.wisp.enableThreadAsWisp", false) :
                parseBooleanParameter(p, "com.alibaba.shiftThreadModel", false);
        ALL_THREAD_AS_WISP = parseBooleanParameter(p, "com.alibaba.wisp.allThreadAsWisp", false);
        STACK_SIZE = parsePositiveIntegerParameter(p, "com.alibaba.wisp.stacksize", 512 * 1024);
        EXTERNAL_SUBMIT_THRESHOLD = parsePositiveIntegerParameter(p, "com.alibaba.wisp.limit", 100);
        PARK_ONE_MS_AT_LEAST = parseBooleanParameter(p, "com.alibaba.wisp.parkOneMs", true);
        USE_THREAD_POOL_LIMIT = parseBooleanParameter(p, "com.alibaba.threadPoolLimit", true);
        GLOBAL_POLLER = parseBooleanParameter(p, "com.alibaba.globalPoller", true);
        WISP_VERSION = parsePositiveIntegerParameter(p, "com.alibaba.wisp.version", 1);
        WORKER_COUNT = parsePositiveIntegerParameter(p, "com.alibaba.wisp.carrierEngines",
                Runtime.getRuntime().availableProcessors());
        ENABLE_HANDOFF = parseBooleanParameter(p, "com.alibaba.wisp.enableHandOff",
                TRANSPARENT_WISP_SWITCH && WISP_VERSION == 2);
        // handoff carrier thread implementation is not stable enough,
        // use preempt by default, and we'll move to ADAPTIVE in the future
        HANDOFF_POLICY = WispSysmon.Policy.valueOf(
                p.getProperty("com.alibaba.wisp.handoffPolicy", WispSysmon.Policy.PREEMPT.name()));
        SYSMON_TICK_US = parsePositiveIntegerParameter(p, "com.alibaba.wisp.sysmonTickUs",
                (int) TimeUnit.MILLISECONDS.toMicros(100));
        MIN_PARK_NANOS = parsePositiveIntegerParameter(p, "com.alibaba.wisp.minParkNanos", 100);
        WISP_PROFILE_LOG_ENABLED = parseBooleanParameter(p, "com.alibaba.wisp.enableProfileLog", false);
        WISP_PROFILE_LOG_INTERVAL_MS = parsePositiveIntegerParameter(p, "com.alibaba.wisp.logTimeInternalMillis", 15000);
        if (WISP_PROFILE_LOG_ENABLED) {
            WISP_PROFILE = true;
            WISP_PROFILE_LOG_PATH = p.getProperty("com.alibaba.wisp.logPath");
        } else {
            WISP_PROFILE = parseBooleanParameter(p, "com.alibaba.wisp.profile", false);
            WISP_PROFILE_LOG_PATH = "";
        }

        CARRIER_AS_POLLER = parseBooleanParameter(p, "com.alibaba.wisp.useCarrierAsPoller", ALL_THREAD_AS_WISP);
        MONOLITHIC_POLL = parseBooleanParameter(p, "com.alibaba.wisp.monolithicPoll", true);
        WISP_HIGH_PRECISION_TIMER = parseBooleanParameter(p, "com.alibaba.wisp.highPrecisionTimer", false);
        WISP_USE_STEAL_LOCK = parseBooleanParameter(p, "com.alibaba.wisp.useStealLock", WISP_VERSION == 2);
        WISP_ENGINE_TASK_CACHE_SIZE = parsePositiveIntegerParameter(p, "com.alibaba.wisp.engineTaskCache", 20);
        WISP_SCHEDULE_STEAL_RETRY = parsePositiveIntegerParameter(p, "com.alibaba.wisp.schedule.stealRetry", Math.max(1, WORKER_COUNT / 2));
        WISP_SCHEDULE_PUSH_RETRY = parsePositiveIntegerParameter(p, "com.alibaba.wisp.schedule.pushRetry", WORKER_COUNT);
        WISP_SCHEDULE_HELP_STEAL_RETRY = parsePositiveIntegerParameter(p, "com.alibaba.wisp.schedule.helpStealRetry", Math.max(1, WORKER_COUNT / 4));
        SCHEDULING_POLICY = Wisp2Scheduler.SchedulingPolicy.valueOf(p.getProperty("com.alibaba.wisp.schedule.policy",
                WORKER_COUNT > 16 ? Wisp2Scheduler.SchedulingPolicy.PUSH.name() : Wisp2Scheduler.SchedulingPolicy.PULL.name()));
        USE_DIRECT_SELECTOR_WAKEUP = parseBooleanParameter(p, "com.alibaba.wisp.directSelectorWakeup", TRANSPARENT_WISP_SWITCH && WISP_VERSION == 2);
        WISP_ENABLE_SOCKET_LOCK = parseBooleanParameter(p, "com.alibaba.wisp.useSocketLock", true);
        CARRIER_GROW = parseBooleanParameter(p, "com.alibaba.wisp.growCarrier", false);
        SYSMON_CARRIER_GROW_TICK_US = parsePositiveIntegerParameter(p, "com.alibaba.wisp.growCarrierTickUs", (int) TimeUnit.SECONDS.toMicros(5));
        checkCompatibility();
    }

    private static void checkCompatibility() {
        if (WISP_VERSION != 1 && WISP_VERSION != 2) {
            throw new IllegalArgumentException("\"-Dcom.alibaba.wisp.version=\" is only allowed to be 1 or 2");
        }
        checkDependency(ENABLE_THREAD_AS_WISP, "-Dcom.alibaba.wisp.enableThreadAsWisp=true",
                TRANSPARENT_WISP_SWITCH, "-Dcom.alibaba.wisp.transparentWispSwitch=true");
        checkDependency(ENABLE_HANDOFF, "-Dcom.alibaba.wisp.enableHandOff=true",
                TRANSPARENT_WISP_SWITCH, "-Dcom.alibaba.wisp.transparentWispSwitch=true");
        checkDependency(WISP_VERSION == 2, "-Dcom.alibaba.wisp.version=2",
                GLOBAL_POLLER, "-Dcom.alibaba.globalPoller=true");
        checkDependency(ALL_THREAD_AS_WISP, "-Dcom.alibaba.wisp.allThreadAsWisp=true",
                ENABLE_THREAD_AS_WISP && WISP_VERSION == 2,
                "-Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.version=2");
        checkDependency(USE_DIRECT_SELECTOR_WAKEUP, "-Dcom.alibaba.wisp.directSelectorWakeup=true",
                WISP_VERSION == 2, "-Dcom.alibaba.wisp.version=2");
        checkDependency(CARRIER_AS_POLLER, "-Dcom.alibaba.wisp.useCarrierAsPoller=true",
                ALL_THREAD_AS_WISP, "-Dcom.alibaba.wisp.allThreadAsWisp=true");
        checkDependency(CARRIER_GROW, "-Dcom.alibaba.wisp.growCarrier",
                WISP_VERSION == 2, "-Dcom.alibaba.wisp.version=2");
    }

    private static void checkDependency(boolean cond, String condStr, boolean preRequire, String preRequireStr) {
        if (cond && !preRequire) {
            throw new IllegalArgumentException("\"" + condStr + "\" depends on \"" + preRequireStr + "\"");
        }
    }

    private static int parsePositiveIntegerParameter(Properties p, String key, int defaultVal) {
        String value;
        if (p == null || (value = p.getProperty(key)) == null) {
            return defaultVal;
        }
        int res = defaultVal;
        try {
            res = Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
        return res <= 0 ? defaultVal : res;
    }

    private static boolean parseBooleanParameter(Properties p, String key, boolean defaultVal) {
        String value;
        if (p == null || (value = p.getProperty(key)) == null) {
            return defaultVal;
        }
        return Boolean.valueOf(value);
    }

    private static List<String> parseListParameter(Properties p, Properties confProp, String key) {
        String value = p.getProperty(key);
        if (value == null) {
            value = confProp.getProperty(key);
        }
        return value == null ? Collections.emptyList() :
                Arrays.asList(value.trim().split(DELIMITER));
    }

    /**
     * Loading config from system property "com.alibaba.wisp.config" specified
     * file or jre/lib/wisp.properties.
     */
    private static void loadBizConfig() {
        Properties p = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Properties>() {
                    public Properties run() {
                        return System.getProperties();
                    }
                }
        );
        String path = p.getProperty("com.alibaba.wisp.config");
        if (path == null || !new File(path).isFile()) {
            path = java.security.AccessController.doPrivileged(
                    new GetPropertyAction("java.home")) +
                    File.separator + "conf" + File.separator + "wisp.properties";
        }

        File f = new File(path);
        if (f.exists()) {
            Properties confProp = new Properties();
            try (InputStream is = new BufferedInputStream(new FileInputStream(f.getPath()))) {
                p.load(is);
            } catch (IOException e) {
                // ignore, all STACK_LIST are empty
            }
            PROXY_SELECTOR_STACK_LIST = parseListParameter(p, confProp, "com.alibaba.wisp.biz.selector");
            PUT_TO_CURRENT_STACK_LIST = parseListParameter(p, confProp, "com.alibaba.wisp.biz.current");
            PUT_TO_MANAGED_THREAD_STACK_LIST = parseListParameter(p, confProp, "com.alibaba.wisp.biz.manage");
            PUT_STACK_BLACKLIST = parseListParameter(p, confProp, "com.alibaba.wisp.biz.black");
            SLEEP_SELECTOR_STACK_LIST = parseListParameter(p, confProp, "com.alibaba.wisp.biz.selector.sleep");
            THREAD_AS_WISP_BLACKLIST = parseListParameter(p, confProp, "com.alibaba.wisp.threadAsWisp.black");
        } else {
            System.out.println(f + " not exist");
        }
    }

    private static final int UNLOADED = 0, LOADING = 1, LOADED = 2;
    private static AtomicInteger bizLoadStatus = new AtomicInteger(UNLOADED);

    private static void ensureBizConfigLoaded() {
        if (bizLoadStatus.get() == LOADED) {
            return;
        }
        if (bizLoadStatus.get() == UNLOADED && bizLoadStatus.compareAndSet(UNLOADED, LOADING)) {
            try {
                loadBizConfig();
            } finally {
                bizLoadStatus.set(LOADED);
            }
        }
        while (bizLoadStatus.get() != LOADED) {/* wait */}
    }

    static boolean ifPutToCurrentEngine() {
        if (ALL_THREAD_AS_WISP) {
            return false;
        }
        ensureBizConfigLoaded();
        StackTraceElement[] bt = Thread.currentThread().getStackTrace();
        return !currentStackContains(PUT_STACK_BLACKLIST, bt) &&
                currentStackContains(PUT_TO_CURRENT_STACK_LIST, bt);
    }

    static boolean ifProxySelector() {
        if (ALL_THREAD_AS_WISP) {
            return false;
        }
        ensureBizConfigLoaded();
        return currentStackContains(PROXY_SELECTOR_STACK_LIST, Thread.currentThread().getStackTrace());
    }

    static boolean ifSpinSelector() {
        if (ALL_THREAD_AS_WISP) {
            return false;
        }
        ensureBizConfigLoaded();
        return currentStackContains(SLEEP_SELECTOR_STACK_LIST, Thread.currentThread().getStackTrace());
    }

    static boolean ifPutToManagedThread() {
        if (ALL_THREAD_AS_WISP) {
            return false;
        }
        ensureBizConfigLoaded();
        StackTraceElement[] bt = Thread.currentThread().getStackTrace();
        for (StackTraceElement aBt : bt) {
            if (aBt.getClassName().equals(WispWorkerContainer.class.getName())) return false;
        }
        return !currentStackContains(PUT_STACK_BLACKLIST, bt) &&
                currentStackContains(PUT_TO_MANAGED_THREAD_STACK_LIST, bt);
    }

    static List<String> getThreadAsWispBlacklist() {
        ensureBizConfigLoaded();
        return THREAD_AS_WISP_BLACKLIST;
    }

    private static boolean currentStackContains(List<String> stacks, StackTraceElement[] bt) {
        for (String clazzAndMethod : stacks) {
            for (StackTraceElement aBt : bt) {
                if (aBt.getClassName().length() + aBt.getMethodName().length() + 2 == clazzAndMethod.length() &&
                        clazzAndMethod.startsWith(aBt.getClassName()) && clazzAndMethod.endsWith(aBt.getMethodName()) &&
                        clazzAndMethod.regionMatches(aBt.getClassName().length(), "::", 0, 2))
                    return true;
            }
        }
        return false;
    }
}
