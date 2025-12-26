package com.arco2121.jasync.AsyncTypes;

import com.arco2121.jasync.AsyncUtility.MissingAsyncException;
import com.arco2121.jasync.AsyncUtility.AsyncInterface;

import java.util.concurrent.*;

public class VirtualAsync implements AsyncInterface {
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
    public <T> Future<T> async(Callable<T> task) {
        return EXEC.submit(task);
    }
    @Override
    public <T> T await(Future<T> future) {
        try {
            return future.get(); // cheap block
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
