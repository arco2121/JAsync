package com.arco2121.jasync.Types.Async;

import com.arco2121.jasync.Types.Interfaces.AsyncInterface;
import com.arco2121.jasync.JAsync.Running.Asyncable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Async for JAVA versions before v21 (17-20)
 */
public final class CompletableAsync implements AsyncInterface {

    private static final ExecutorService EXEC =
            Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors()
            );

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
    public final <T> Asyncable<List<T>> awaitSafeAll(Asyncable<T>... tasks) {
        CompletableFuture<?>[] futures = Arrays.stream(tasks).map(Asyncable::delegate).toArray(CompletableFuture[]::new);
        CompletableFuture<List<T>> combinedFuture = CompletableFuture.allOf(futures).thenApply(v -> Arrays.stream(tasks).map(t -> t.getDelegate().join()).collect(Collectors.toList()));
        return new Asyncable<>(combinedFuture);
    }

    @Override
    public final <T> Asyncable<T> awaitSafeAny(Asyncable<T>... tasks) {
        CompletableFuture<T>[] futures = Arrays.stream(tasks).map(Asyncable::delegate).toArray(CompletableFuture[]::new);
        CompletableFuture<T> firstSuccess = new CompletableFuture<>();
        AtomicInteger failureCount = new AtomicInteger(0);
        for (CompletableFuture<T> f : futures) {
            f.handle((result, ex) -> {
                if (ex == null)
                    firstSuccess.complete(result);
                else {
                    if (failureCount.incrementAndGet() == futures.length)
                        firstSuccess.completeExceptionally(new RuntimeException("All tasks failed"));
                }
                return null;
            });
        }
        return new Asyncable<>(firstSuccess);
    }

    @SafeVarargs
    @Override
    public final <T> Asyncable<T> awaitSafeRace(Asyncable<T>... tasks) {
        CompletableFuture<?>[] futures = Arrays.stream(tasks).map(Asyncable::delegate).toArray(CompletableFuture[]::new);
        return new Asyncable<>((CompletableFuture<T>) CompletableFuture.anyOf(futures));
    }

    @SafeVarargs
    @Override
    public final Asyncable<List<?>> awaitAll(Asyncable<?>... tasks) {
        CompletableFuture<?>[] futures = Arrays.stream(tasks).map(Asyncable::delegate).toArray(CompletableFuture[]::new);
        CompletableFuture<List<?>> combinedFuture = CompletableFuture.allOf(futures).thenApply(v -> Arrays.stream(tasks).map(t -> t.getDelegate().join()).collect(Collectors.toList()));
        return new Asyncable<>(combinedFuture);
    }

    @Override
    public Asyncable<?> awaitRace(Asyncable<?>... tasks) {
        CompletableFuture<?>[] futures = Arrays.stream(tasks).map(Asyncable::delegate).toArray(CompletableFuture[]::new);
        return new Asyncable<>(CompletableFuture.anyOf(futures));
    }

    @Override
    public Asyncable<?> awaitAny(Asyncable<?>... tasks) {
        CompletableFuture<?>[] futures = Arrays.stream(tasks).map(Asyncable::delegate).toArray(CompletableFuture[]::new);
        CompletableFuture<Object> firstSuccess = new CompletableFuture<>();
        AtomicInteger failureCount = new AtomicInteger(0);
        for (CompletableFuture<?> f : futures) {
            f.handle((result, ex) -> {
                if (ex == null)
                    firstSuccess.complete(result);
                else {
                    if (failureCount.incrementAndGet() == futures.length)
                        firstSuccess.completeExceptionally(new RuntimeException("All tasks failed"));
                }
                return null;
            });
        }
        return new Asyncable<>(firstSuccess);
    }

    @Override
    public <T> T await(Asyncable<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}