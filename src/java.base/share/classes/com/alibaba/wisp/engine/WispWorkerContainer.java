package com.alibaba.wisp.engine;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Lots of networking library maintain their own thread model.
 * Instead, we create CPU-number befitted threads and letting user's
 * code running in a coroutine of our maintained threads.
 */
public enum WispWorkerContainer {
    INSTANCE;

    public static final String WISP_THREAD_NAME_PREFIX = "WISP_WORKER_";

    private List<WispEngine> workers = new ArrayList<>();
    private ConcurrentHashMap<String, Dispatcher> id2dispatcher = new ConcurrentHashMap<>();

    public List<WispEngine> getWorkers() {
        return workers;
    }

    WispWorkerContainer() {
        if (WispConfiguration.WISP_VERSION == 2 || !WispEngine.transparentWispSwitch()) {
            return;
        }
        // pre-start threads.
        CountDownLatch countDownLatch = new CountDownLatch(WispConfiguration.WORKER_COUNT);
        List<Thread> workerList = IntStream.range(0, WispConfiguration.WORKER_COUNT).boxed().map(i -> {
            Thread t = new Thread(null, new Runnable() { // convert to lambda leading to block(jvm BUG?)
                @Override
                public void run() {
                    WispEngine engine = WispEngine.current(); // create engine
                    engine.registerPerfCounter();
                    countDownLatch.countDown();
                    engine.schedule();
                    // wait task submit
                }
            }, WISP_THREAD_NAME_PREFIX + i);
            t.setDaemon(true);
            t.start();
            return t;
        }).collect(Collectors.toList());
        try { // wait engine create finish
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        workers = workerList.stream().map(WispEngine.JLA::getWispEngine).collect(Collectors.toList());
    }

    /**
     * Create a coroutine running in the global workers
     *
     * @param group in which coroutines will be dispatched in Round-robin into workers.
     * @param name  name as return value for Thread.currentThread().getName()
     * @param r     target code
     * @param t     as return value for Thread.currentThread()
     */
    public void dispatch(String group, String name, Runnable r, Thread t) {
        WispEngine wispEngine;
        assert WispEngine.transparentWispSwitch();
        if (WispConfiguration.WISP_VERSION == 2) {
            wispEngine = WispEngine.current();
        } else {
            Dispatcher dispatcher = id2dispatcher.get(group);
            if (dispatcher == null) {
                id2dispatcher.putIfAbsent(group, new Dispatcher());
                dispatcher = id2dispatcher.get(group);
            }
            wispEngine = dispatcher.nextEngine();
        }
        wispEngine.dispatchTask(r, name, t);
    }

    private class Dispatcher {
        AtomicInteger cnt = new AtomicInteger(0);
        int[] order;

        Dispatcher() {
            generateOrder();
        }

        private void generateOrder() {
            List<Integer> list = IntStream.range(0, workers.size()).boxed().collect(Collectors.toList());
            Collections.shuffle(list);
            order = list.stream().mapToInt(i -> i).toArray();
        }

        WispEngine nextEngine() {
            return workers.get(order[cnt.getAndIncrement() % order.length]);
        }
    }

    /**
     * make group for thread according to its name.
     *
     * Example:
     * Druid-ConnectionPool-CreateScheduler--1-Thread-4758
     * -->
     * DruidConnectionPoolCreateSchedulerThread
     */
    static String getThreadUsage(String threadName) {
        return Arrays.stream(threadName.split("[# \\-_.]")).filter(s -> !s.matches("\\d+(\\.\\d+)?"))
                .reduce((s1, s2) -> s1 + s2).orElse("null");
    }
}
