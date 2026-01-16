package com.arco2121.jasync.AsyncTypes;

import com.arco2121.jasync.JAsync.Asyncable;

import java.util.List;
import java.util.concurrent.Callable;

public interface AsyncInterface {
    <T> Asyncable<T> async(Callable<T> task);
    <T> T await(Asyncable<T> task);
    <T> T await(Asyncable<T> task, int timeout);
    <T> Asyncable<List<T>> awaitSafeAll(Asyncable<T>... tasks);
    <T> Asyncable<T> awaitSafeRace(Asyncable<T>... tasks);
    <T> Asyncable<T> awaitSafeAny(Asyncable<T>... tasks);
    Asyncable<List<?>> awaitAll(Asyncable<?>... tasks);
    Asyncable<?> awaitRace(Asyncable<?>... tasks);
    Asyncable<?> awaitAny(Asyncable<?>... tasks);
}