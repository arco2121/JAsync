package com.arco2121.jasync.Async;

import java.util.concurrent.Callable;

public interface AsyncInterface {
    <T> AsyncT<T> async(Callable<T> task);
    <T> T await(AsyncT<T> task);
    <T> T await(AsyncT<T> task, int timeout);
}