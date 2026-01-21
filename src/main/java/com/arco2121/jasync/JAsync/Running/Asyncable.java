package com.arco2121.jasync.JAsync.Running;

import com.arco2121.jasync.Types.Exceptions.MissingAsyncException;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represent an Async function type
 * @param delegate
 * @param <T>
 */
public record Asyncable<T>(CompletableFuture<T> delegate) implements Future<T>, Callable<T>, Runnable {

    public Asyncable {
        if (delegate == null) {
            throw new MissingAsyncException("Cannot be null");
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }

    @Override
    public T call() throws Exception {
        return delegate.get();
    }

    @Override
    public void run() {
        try {
            delegate.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public <R> Asyncable<R> then(Function<? super T, ? extends R> mapper) {
        return new Asyncable<>(delegate.thenApply(mapper));
    }
    public Asyncable<Void> then(Runnable mapper) {
        return new Asyncable<>(delegate.thenRun(mapper));
    }

    public Asyncable<Void> finish(Consumer<? super T> action) {
        return new Asyncable<>(delegate.thenAccept(action));
    }

    public Asyncable<T> error(Function<Throwable, ? extends T> errorHandler) {
        return new Asyncable<>(delegate.exceptionally(errorHandler));
    }
    public Asyncable<Void> error(Consumer<Throwable> errorHandler) {
        return new Asyncable<>(delegate.handle((result, ex) -> {
            if (ex != null) {
                errorHandler.accept(ex);
            }
            return null;
        }));
    }

    public CompletableFuture<T> getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        try {
            return this.get().toString();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
