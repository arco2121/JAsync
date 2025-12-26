package com.arco2121.jasync.AsyncTypes;

import com.arco2121.jasync.AsyncUtility.AsyncInterface;

import java.util.concurrent.*;

public class OldAsync implements AsyncInterface {
    private static final ExecutorService EXEC =
            Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors()
            );

    @Override
    public <T> Future<T> async(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, EXEC);
    }
    @Override
    public <T> T await(Future<T> future) {
        try {
            return future.get(); // blocking, expensive pre-21
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}