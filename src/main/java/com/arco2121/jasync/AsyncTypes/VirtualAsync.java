package com.arco2121.jasync.AsyncTypes;

import com.arco2121.jasync.Async.AsyncT;
import com.arco2121.jasync.Async.MissingAsyncException;
import com.arco2121.jasync.Async.AsyncInterface;

import java.util.concurrent.*;

public final class VirtualAsync implements AsyncInterface {
    private static final ExecutorService EXEC;
    static {
        ExecutorService tmp;
        try {
            tmp = (ExecutorService) Executors.class
                    .getMethod("newVirtualThreadPerTaskExecutor")
                    .invoke(null);
        } catch (Exception e) {
            throw  new MissingAsyncException("Need Java 21 or above for VirtualAsync");
        }
        EXEC = tmp;
    }

    @Override
    public <T> AsyncT<T> async(Callable<T> task) {
        return (AsyncT<T>) EXEC.submit(task);
    }
    @Override
    public <T> T await(AsyncT<T> future) {
        try {
            return future.get(); // cheap block
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
    @Override
    public <T> T await(AsyncT<T> task, int timeout) {
        try {
            return task.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (TimeoutException e) {
            System.err.println("Timeout task: " + e.getMessage());
            return null;
        }
    }
}
