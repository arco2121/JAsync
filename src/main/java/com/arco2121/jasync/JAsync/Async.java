package com.arco2121.jasync.JAsync;

import com.arco2121.jasync.Types.Async.CompletableAsync;
import com.arco2121.jasync.Types.Async.VirtualAsync;
import com.arco2121.jasync.Types.Exceptions.ThrowRunnable;
import com.arco2121.jasync.Types.Exceptions.ThrowCallable;
import com.arco2121.jasync.Types.Interfaces.AsyncInterface;
import com.arco2121.jasync.JAsync.Running.AsyncInterval;
import com.arco2121.jasync.JAsync.Running.Asyncable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Async declaration
 */
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
            return defaultCanon.criteria() ? new CompletableAsync() : new VirtualAsync();
        }
        return new CompletableAsync();
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
    public static Asyncable<Void> async(Runnable task) {
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
    public static <T> T await(Callable<T> task, long maxTimeout) {
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
     * Try Catch block, extension of the .error method of the Async function type
     * @param trying
     * @param handle
     * @return
     * @param <T>
     */
    public static <T> Asyncable<T> tryCatch(ThrowCallable<T> trying, Function<Throwable, T> handle) {
        return async(() -> {
            try { return trying.call(); } catch (Exception e) { throw e; }
        }).error(handle);
    }
    public static void tryCatch(ThrowRunnable trying, Consumer<Throwable> handle) {
        async(() -> {
            trying.run();
            return null;
        }).error(e -> {
            handle.accept(e);
            return null;
        });
    }

    /**
     * Execute with delay a function on a separated thread
     * @param task
     * @param timeout
     */
    public static void delayed(Runnable task, long timeout) {
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
    public static <T> T timeout(Callable<T> task, long waitFor) throws InterruptedException {
        Thread.sleep(waitFor);
        return await(task);
    }
    public static void timeout(long waitFor) {
        try {
            Thread.sleep(waitFor);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static void timeout(Runnable task, long waitFor) throws InterruptedException {
        Thread.sleep(waitFor);
        task.run();
    }

    /**
     * Execute multiple functions in sequence
     * @return Asyncable
     * @param <T>
     * @param <R>
     */
    @SuppressWarnings("unchecked")
    public static <T, R> Asyncable<R> pipe(Callable<T> begin, Function<?, ?>... transformations) {
        if (transformations.length == 0) {
            return (Asyncable<R>) async(begin);
        }
        Asyncable<?> pipeline = async(begin);
        for (Function<?, ?> step : transformations) {
            Function<Object, Object> uncheckedStep = (Function<Object, Object>) step;
            pipeline = pipeline.then(uncheckedStep);
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
     * @return Collection of AsyncInterval objects
     */
    public static Collection<AsyncInterval> knownIntervals() {
        return INTERVALS.values();
    }
}