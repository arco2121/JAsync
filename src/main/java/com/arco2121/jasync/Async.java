package com.arco2121.jasync;

import com.arco2121.jasync.AsyncTypes.*;
import com.arco2121.jasync.AsyncUtility.AsyncInterface;

import java.util.concurrent.*;

public final class Async {
    //Internal Structure
    @FunctionalInterface
    public interface Canon {
        boolean criteria();
    }

    private static AsyncInterface SELECTOR = select();
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

    //Public methods
    /***
     *
     * @param canone It specifies the canon to use to decide the type of Async to use: true=Old, false=New
     */
    public static void selectCriteria(Canon canone) {
        defaultCanon = canone;
    }

    public static <T> Future<T> async(Callable<T> task) {
        return SELECTOR.async(task);
    }
    public static Future<?> async(Runnable task) {
        return SELECTOR.async(Executors.callable(task, null));
    }
    public static <T> T await(Future<T> future) {
        return SELECTOR.await(future);
    }
}