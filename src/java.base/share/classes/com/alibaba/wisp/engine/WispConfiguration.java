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
    static final int SYSMON_TICK_US;
    // monitor
    static final boolean WISP_PROFILE_DETAIL;
    static final boolean PERF_LOG_ENABLED;
    static final int PERF_LOG_INTERVAL_MS;
    static final String PERF_LOG_PATH;

    // wisp2
    static final boolean WISP_HIGH_PRECISION_TIMER;
    static final boolean WISP_DAEMON_WORKER;
    static final boolean WISP_USE_STEAL_LOCK;
    static final int WISP_ENGINE_TASK_CACHE_SIZE;
    static final int WISP_SCHEDULE_STEAL_RETRY;
    static final int WISP_SCHEDULE_PUSH_RETRY;
    static final int WISP_SCHEDULE_HELP_STEAL_RETRY;


    private static List<String> PROXY_SELECTOR_STACK_LIST;
    private static List<String> SLEEP_SELECTOR_STACK_LIST;
    private static List<String> PUT_TO_CURRENT_STACK_LIST;
    private static List<String> PUT_TO_MANAGED_THREAD_STACK_LIST;
    private static List<String> PUT_STACK_BLACKLIST;

    static {
        Properties p = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Properties>() {
                    public Properties run() {
                        return System.getProperties();
                    }
                }
        );

        final int CORES = Runtime.getRuntime().availableProcessors();
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
        WORKER_COUNT = parsePositiveIntegerParameter(p, "com.alibaba.wisp.carrierEngines", CORES);
        ENABLE_HANDOFF = parseBooleanParameter(p, "com.alibaba.wisp.enableHandOff", false);
        SYSMON_TICK_US = parsePositiveIntegerParameter(p, "com.alibaba.wisp.sysmonTickUs",
                (int) TimeUnit.SECONDS.toMicros(1));
        PERF_LOG_ENABLED = parseBooleanParameter(p, "com.alibaba.wisp.enablePerfLog", false);
        PERF_LOG_INTERVAL_MS = parsePositiveIntegerParameter(p, "com.alibaba.wisp.logTimeInternalMillis", 15000);
        if (PERF_LOG_ENABLED) {
            WISP_PROFILE_DETAIL = true;
            PERF_LOG_PATH = p.getProperty("com.alibaba.wisp.logPath");
        } else {
            WISP_PROFILE_DETAIL = parseBooleanParameter(p, "com.alibaba.wisp.profileDetail", false);
            PERF_LOG_PATH = "";
        }

        WISP_HIGH_PRECISION_TIMER = parseBooleanParameter(p, "com.alibaba.wisp.highPrecisionTimer", false);
        WISP_DAEMON_WORKER = parseBooleanParameter(p, "com.alibaba.wisp.daemonWorker", true);
        WISP_USE_STEAL_LOCK = parseBooleanParameter(p, "com.alibaba.wisp.useStealLock", true);
        WISP_ENGINE_TASK_CACHE_SIZE = parsePositiveIntegerParameter(p, "com.alibaba.wisp.engineTaskCache", 20);
        WISP_SCHEDULE_STEAL_RETRY = parsePositiveIntegerParameter(p, "com.alibaba.wisp.schedule.stealRetry", CORES);
        WISP_SCHEDULE_PUSH_RETRY = parsePositiveIntegerParameter(p, "com.alibaba.wisp.schedule.pushRetry", CORES);
        WISP_SCHEDULE_HELP_STEAL_RETRY = parsePositiveIntegerParameter(p, "com.alibaba.wisp.schedule.helpStealRetry",
                Math.max(1, CORES / 2));
        checkCompatibility();
    }

    private static void checkCompatibility() {
        if (WISP_VERSION != 1 && WISP_VERSION != 2) {
            throw new IllegalArgumentException("\"-Dcom.alibaba.wisp.version=\" is only allowed to be 1 or 2");
        }
        checkDependency(ENABLE_THREAD_AS_WISP, "-Dcom.alibaba.wisp.enableThreadAsWisp=true",
                TRANSPARENT_WISP_SWITCH, "-Dcom.alibaba.wisp.transparentWispSwitch=true");
        checkDependency(ENABLE_HANDOFF, "-Dcom.alibaba.wisp.enableHandOff=true",
                TRANSPARENT_WISP_SWITCH, "-Dcom.alibaba.wisp.enableThreadAsWisp=true");
        checkDependency(WISP_VERSION == 2, "-Dcom.alibaba.wisp.version=2",
                GLOBAL_POLLER, "-Dcom.alibaba.globalPoller=true");
        checkDependency(ALL_THREAD_AS_WISP, "-Dcom.alibaba.wisp.allThreadAsWisp=true",
                ENABLE_THREAD_AS_WISP && WISP_VERSION == 2,
                "-Dcom.alibaba.wisp.enableThreadAsWisp=true -Dcom.alibaba.wisp.version=2");
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

    private static List<String> parseListParameter(Properties p, String key) {
        String value = p.getProperty(key);
        return value == null ? Collections.emptyList() :
                Arrays.asList(value.trim().split(DELIMITER));
    }

    /**
     * Loading config from system property "com.alibaba.wisp.config" specified
     * file or jre/lib/wisp.properties.
     */
    private static void loadBizConfig() {
        String path = java.security.AccessController.doPrivileged(
                new GetPropertyAction("com.alibaba.wisp.config"));
        if (path == null || !new File(path).isFile()) {
            path = java.security.AccessController.doPrivileged(
                    new GetPropertyAction("java.home")) +
                    File.separator + "conf" + File.separator + "wisp.properties";
        }

        File f = new File(path);
        if (f.exists()) {
            Properties p = new Properties();
            try (InputStream is = new BufferedInputStream(new FileInputStream(f.getPath()))) {
                p.load(is);
            } catch (IOException e) {
                // ignore, all STACK_LIST are empty
            }
            PROXY_SELECTOR_STACK_LIST = parseListParameter(p, "com.alibaba.wisp.biz.selector");
            PUT_TO_CURRENT_STACK_LIST = parseListParameter(p, "com.alibaba.wisp.biz.current");
            PUT_TO_MANAGED_THREAD_STACK_LIST = parseListParameter(p, "com.alibaba.wisp.biz.manage");
            PUT_STACK_BLACKLIST = parseListParameter(p, "com.alibaba.wisp.biz.black");
            SLEEP_SELECTOR_STACK_LIST = parseListParameter(p, "com.alibaba.wisp.biz.selector.sleep");
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
        StackTraceElement bt[] = Thread.currentThread().getStackTrace();
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
        StackTraceElement bt[] = Thread.currentThread().getStackTrace();
        for (StackTraceElement aBt : bt) {
            if (aBt.getClassName().equals(WispWorkerContainer.class.getName())) return false;
        }
        return !currentStackContains(PUT_STACK_BLACKLIST, bt) &&
                currentStackContains(PUT_TO_MANAGED_THREAD_STACK_LIST, bt);
    }

    private static boolean currentStackContains(List<String> stacks, StackTraceElement bt[]) {
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
