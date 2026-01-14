package com.arco2121.jasync.AsyncTypes;

import com.arco2121.jasync.Async.AsyncInterface;
import com.arco2121.jasync.Async.AsyncT;

import java.util.concurrent.*;

public final class OldAsync implements AsyncInterface {
    private static final ExecutorService EXEC =
            Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors()
            );

    @Override
    public <T> AsyncT<T> async(Callable<T> task) {
        return (AsyncT<T>) CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, EXEC);
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

    @Override
    public <T> T await(AsyncT<T> future) {
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