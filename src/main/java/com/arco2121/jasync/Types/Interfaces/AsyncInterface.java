package com.arco2121.jasync.Types.Interfaces;

import com.arco2121.jasync.JAsync.Running.Asyncable;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Represent a valid Async handler
 */
public interface AsyncInterface {

    <T> Asyncable<T> async(Callable<T> task);
    <T> T await(Asyncable<T> task);
    <T> T await(Asyncable<T> task, long timeout);
    @SuppressWarnings("unchecked")
    <T> Asyncable<List<T>> awaitSafeAll(Asyncable<T>... tasks);
    @SuppressWarnings("unchecked")
    <T> Asyncable<T> awaitSafeRace(Asyncable<T>... tasks);
    @SuppressWarnings("unchecked")
    <T> Asyncable<T> awaitSafeAny(Asyncable<T>... tasks);
    Asyncable<List<?>> awaitAll(Asyncable<?>... tasks);
    Asyncable<?> awaitRace(Asyncable<?>... tasks);
    Asyncable<?> awaitAny(Asyncable<?>... tasks);
}