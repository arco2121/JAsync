package com.arco2121.jasync.Async;

import com.arco2121.jasync.AsyncTypes.*;

import java.util.*;
import java.util.concurrent.*;

public final class Async {
    //Internal Structure
    @FunctionalInterface
    public interface Canon {
        boolean criteria();
    }

    private static com.arco2121.jasync.Async.AsyncInterface SELECTOR = select();
    private static Canon defaultCanon = () -> {
        int v = Runtime.version().feature();
        return (v < 21);
    };
    private static com.arco2121.jasync.Async.AsyncInterface select() {
        if (defaultCanon != null) {
            return defaultCanon.criteria() ? new OldAsync() : new VirtualAsync();
        }
        return new OldAsync();
    }
    private final static ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
    private final static Map<Integer, AsyncIntervalT> INTERVALS = new HashMap<>();

    //Public methods
    /***
     *
     * @param canone It specifies the canon to use to decide the type of Async to use: true=Old, false=New
     */
    public static void selectCriteria(Canon canone) {
        defaultCanon = canone;
    }

    /**
     * Create an Async function
     * @param task
     * @return AsyncT
     * @param <T>
     */
    public static <T> com.arco2121.jasync.Async.AsyncT<T> async(Callable<T> task) {
        return SELECTOR.async(task);
    }

    /**
     * Create an Async function
     * @param task
     * @return AsyncT
     */
    public static com.arco2121.jasync.Async.AsyncT<?> async(Runnable task) {
        return SELECTOR.async(Executors.callable(task, null));
    }

    /**
     * Await a function
     * @param task
     * @return T, null
     * @param <T>
     */
    public static <T> T await(Callable<T> task) {
        return SELECTOR.await((com.arco2121.jasync.Async.AsyncT<T>) task);
    }

    /**
     * Await a function
     * @param task
     * @param maxTimeout
     * @return T, null
     * @param <T>
     */
    public static <T> T await(Callable<T> task, int maxTimeout) {
        return SELECTOR.await((com.arco2121.jasync.Async.AsyncT<T>)task, maxTimeout);
    }

    /**
     * Execute with delay a function on a separated thread
     * @param task
     * @param timeout
     */
    public static void delayed(Runnable task, int timeout) {
        SCHEDULER.schedule(task, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Execute with delay a function on the same Thread
     * @param task
     * @param waitFor
     * @return
     * @param <T>
     * @throws InterruptedException
     */
    public static <T> T timeout(Callable<T> task, int waitFor) throws InterruptedException {
        Thread.sleep(waitFor);
        return await(task);
    }

    /**
     * Create a separate interval
     * @param task
     * @param interval
     * @return
     */
    public static AsyncIntervalT interval(Runnable task, int interval) {
        AsyncIntervalT serving = new AsyncIntervalT((com.arco2121.jasync.Async.AsyncT<?>) task, interval);
        serving.start();
        INTERVALS.put(serving.intervalId, serving);
        return serving;
    }

    /**
     * Clear and interval
     * @param interval
     * @return
     */
    public static boolean clearInterval(AsyncIntervalT interval) {
        try {
            interval.stop();
            INTERVALS.remove(interval.intervalId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean clearInterval(AsyncIntervalT interval, boolean instantaneous) {
        try {
            if(instantaneous) interval.interrupt();
            else interval.stop();
            INTERVALS.remove(interval.intervalId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean clearInterval(int intervalId) {
        try {
            AsyncIntervalT serving = INTERVALS.get(intervalId);
            serving.stop();
            INTERVALS.remove(intervalId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clear all existing intervals
     * @return Un cazzo
     */
    public static boolean clearAllIntervals() {
        try {
            for(AsyncIntervalT interval : INTERVALS.values()) {
                interval.stop();
            }
            INTERVALS.clear();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean clearAllIntervals(boolean instantaneous) {
        try {
            for(AsyncIntervalT interval : INTERVALS.values()) {
                if(instantaneous) interval.interrupt();
                else interval.stop();
            }
            INTERVALS.clear();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}