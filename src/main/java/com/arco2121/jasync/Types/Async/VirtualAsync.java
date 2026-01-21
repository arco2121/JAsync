package com.arco2121.jasync.Types.Async;

import com.arco2121.jasync.Types.Exceptions.MissingAsyncException;
import com.arco2121.jasync.Types.Interfaces.AsyncInterface;
import com.arco2121.jasync.JAsync.Running.Asyncable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Async for JAVA >= v21
 */
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
    public <T> Asyncable<T> async(Callable<T> task) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, EXEC);
        return new Asyncable<>(future);
    }
    @Override
    public <T> T await(Asyncable<T> future) {
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
    public <T> T await(Asyncable<T> task, long timeout) {
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
    public <T> Asyncable<List<T>> awaitSafeAll(Asyncable<T>... tasks) {
        CompletableFuture<T>[] futures = Arrays.stream(tasks).map(Asyncable::getDelegate).toArray(CompletableFuture[]::new);
        CompletableFuture<List<T>> combined = CompletableFuture.allOf(futures).thenApply(v -> Arrays.stream(tasks).map(t -> t.getDelegate().join()).collect(Collectors.toList()));
        return new Asyncable<>(combined);
    }

    @Override
    public <T> Asyncable<T> awaitSafeRace(Asyncable<T>... tasks) {
        CompletableFuture<T>[] futures = Arrays.stream(tasks).map(Asyncable::getDelegate).toArray(CompletableFuture[]::new);
        return new Asyncable<>((CompletableFuture<T>) CompletableFuture.anyOf(futures));
    }

    @Override
    public <T> Asyncable<T> awaitSafeAny(Asyncable<T>... tasks) {
        CompletableFuture<T> firstSuccess = new CompletableFuture<>();
        AtomicInteger failureCount = new AtomicInteger(0);
        for (Asyncable<T> t : tasks) {
            t.getDelegate().handle((res, ex) -> {
                if (ex == null) {
                    firstSuccess.complete(res);
                } else {
                    if (failureCount.incrementAndGet() == tasks.length) {
                        firstSuccess.completeExceptionally(new RuntimeException("All tasks failed"));
                    }
                }
                return null;
            });
        }
        return new Asyncable<>(firstSuccess);
    }

    @Override
    public Asyncable<List<?>> awaitAll(Asyncable<?>... tasks) {
        CompletableFuture<?>[] futures = Arrays.stream(tasks).map(Asyncable::getDelegate).toArray(CompletableFuture[]::new);
        CompletableFuture<List<?>> combined = CompletableFuture.allOf(futures).thenApply(v -> Arrays.stream(tasks).map(t -> t.getDelegate().join()).collect(Collectors.toList()));
        return new Asyncable<>(combined);
    }

    @Override
    public Asyncable<?> awaitRace(Asyncable<?>... tasks) {
        CompletableFuture<?>[] futures = Arrays.stream(tasks).map(Asyncable::getDelegate).toArray(CompletableFuture[]::new);
        return new Asyncable<>(CompletableFuture.anyOf(futures));
    }

    @Override
    public Asyncable<?> awaitAny(Asyncable<?>... tasks) {
        CompletableFuture<Object> firstSuccess = new CompletableFuture<>();
        AtomicInteger failureCount = new AtomicInteger(0);
        for (Asyncable<?> t : tasks) {
            t.getDelegate().handle((res, ex) -> {
                if (ex == null) {
                    firstSuccess.complete(res);
                } else {
                    if (failureCount.incrementAndGet() == tasks.length) {
                        firstSuccess.completeExceptionally(new RuntimeException("All tasks failed"));
                    }
                }
                return null;
            });
        }
        return new Asyncable<>(firstSuccess);
    }
}
