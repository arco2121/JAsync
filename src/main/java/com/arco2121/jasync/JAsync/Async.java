package com.arco2121.jasync.JAsync;

import com.arco2121.jasync.AsyncTypes.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Async {
    //Internal Structure
    @FunctionalInterface
    public interface Canon {
        boolean criteria();
    }

    private final static AsyncInterface SELECTOR = select();
    private static Canon defaultCanon = () -> {
        int v = Runtime.version().feature();
        return (v < 21);
    };
    private static AsyncInterface select() {
        if (defaultCanon != null) {
            return defaultCanon.criteria() ? new OldAsync() : new VirtualAsync();
        }
        return new OldAsync();
    }
    private final static ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
    private final static Map<Integer, AsyncInterval> INTERVALS = new HashMap<>();

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
    public static <T> Asyncable<T> async(Callable<T> task) {
        return SELECTOR.async(task);
    }
    public static Asyncable<?> async(Runnable task) {
        return SELECTOR.async(Executors.callable(task, null));
    }

    /**
     * Await a function
     * @param task
     * @return T, null
     * @param <T>
     */
    public static <T> T await(Callable<T> task) {
        return SELECTOR.await((Asyncable<T>) task);
    }
    public static <T> T await(Callable<T> task, int maxTimeout) {
        return SELECTOR.await((Asyncable<T>)task, maxTimeout);
    }

    /**
     *Await many functions, all the functions that do not fail will be returned
     * @param tasks
     * @return ?
     */
    public static Asyncable<List<?>> awaitAll(Asyncable<?>... tasks) {
        return SELECTOR.awaitAll(tasks);
    }
    /**
     *Await many functions, all the functions that do not fail will be returned
     * @param tasks
     * @return T
     * @param <T>
     */
    @SafeVarargs
    public static <T> Asyncable<List<T>> awaitSafeAll(Asyncable<T>... tasks) {
        return SELECTOR.awaitSafeAll(tasks);
    }

    /**
     * Await many functions, the first one that doesn't fail will be returned
     * @param tasks
     * @return ?
     */
    public static Asyncable<?> awaitRace(Asyncable<?>... tasks) {
        return SELECTOR.awaitRace(tasks);
    }
    /**
     * Await many functions, the first one that doesn't fail will be returned
     * @param tasks
     * @return T
     * @param <T>
     */
    @SafeVarargs
    public static <T> Asyncable<T> awaitSafeRace(Asyncable<T>... tasks) {
        return SELECTOR.awaitSafeRace(tasks);
    }

    /**
     * Await many functions, the first one that doesn't fail will be returned, if one fail all will fail
     * @param tasks
     * @return ?
     */
    public static Asyncable<?> awaitAny(Asyncable<?>... tasks) {
        return SELECTOR.awaitAny(tasks);
    }
    /**
     * Await many functions, the first one that doesn't fail will be returned, if one fail all will fail
     * @param tasks
     * @return T
     * @param <T>
     */
    @SafeVarargs
    public static <T> Asyncable<T> awaitSafeAny(Asyncable<T>... tasks) {
        return SELECTOR.awaitSafeAny(tasks);
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
     * @return T
     * @param <T>
     * @throws InterruptedException
     */
    public static <T> T timeout(Callable<T> task, int waitFor) throws InterruptedException {
        Thread.sleep(waitFor);
        return await(task);
    }
    public static void timeout(int waitFor) {
        try {
            Thread.sleep(waitFor);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static void timeout(Runnable task, int waitFor) throws InterruptedException {
        Thread.sleep(waitFor);
        task.run();
    }

    /**
     * Execute multiple functions in sequence
     * @return AsyncT
     * @param <T>
     * @param <R>
     */
    public static <T, R> Asyncable<R> pipe(Callable<T> begin, Function<Object, Object>... transformations) {
        Asyncable<Object> pipeline = (Asyncable<Object>) async(begin);
        for (Function<Object, Object> step : transformations) {
            pipeline = pipeline.then(step);
        }
        return (Asyncable<R>) pipeline;
    }

    public static <T> AsyncQueue<T> receive(Callable<T> begin, Function<Object, Object>... transformations) {
        Asyncable<Object> pipeline = (Asyncable<Object>) async(begin);
        for (Function<Object, Object> step : transformations) {
            pipeline = pipeline.then(step);
        }
        return (Asyncable<R>) pipeline;
    }

    /**
     * Create an interval
     * @param task
     * @param interval
     * @return AsyncIntervalT
     */
    public static synchronized AsyncInterval interval(Runnable task, int interval) {
        AsyncInterval serving = new AsyncInterval(task, interval);
        return interval(serving);
    }
    public static synchronized AsyncInterval interval(AsyncInterval interval) {
        interval.start();
        INTERVALS.put(interval.getIntervalId(), interval);
        return interval;
    }
    public static synchronized AsyncInterval interval(Consumer<AsyncInterval> task, int interval) {
        AtomicReference<AsyncInterval> intervalRef = new AtomicReference<>();
        Runnable capsule = () -> {
            AsyncInterval instance = intervalRef.get();
            if (instance != null) {
                task.accept(instance);
            }
        };
        return interval(capsule, interval);
    }

    /**
     * Clear an interval
     * @param interval
     * @return boolean
     */
    public static synchronized boolean clearInterval(AsyncInterval interval) {
        try {
            interval.stop();
            INTERVALS.remove(interval.getIntervalId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean clearInterval(AsyncInterval interval, boolean instantaneous) {
        try {
            if(instantaneous) interval.interrupt();
            else interval.stop();
            INTERVALS.remove(interval.getIntervalId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static synchronized boolean clearInterval(int intervalId) {
        try {
            AsyncInterval serving = INTERVALS.get(intervalId);
            serving.stop();
            INTERVALS.remove(intervalId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clear all existing intervals
     * @return boolean
     */
    public static synchronized boolean clearAllIntervals() {
        try {
            for(AsyncInterval interval : INTERVALS.values())
                interval.stop();
            INTERVALS.clear();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean clearAllIntervals(boolean instantaneous) {
        try {
            for(AsyncInterval interval : INTERVALS.values()) {
                if(instantaneous) interval.interrupt();
                else interval.stop();
            }
            INTERVALS.clear();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * View active intervals
     * @return Collection<AsyncIntervalT>
     */
    public static Collection<AsyncInterval> knownIntervals() {
        return INTERVALS.values();
    }
}