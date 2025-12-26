package com.arco2121.jasync.AsyncUtility;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface AsyncInterface {
    <T> Future<T> async(Callable<T> task);
    <T> T await(Future<T> task);
}