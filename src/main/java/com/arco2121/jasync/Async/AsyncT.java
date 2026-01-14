package com.arco2121.jasync.Async;

import java.util.concurrent.*;

public final class AsyncT<T> extends CompletableFuture<T> implements Future<T>, Callable<T>, Runnable {

    private final CompletableFuture<T> delegate;

    public AsyncT(CompletableFuture<T> delegate) {
        if (delegate == null) {
            throw new MissingAsyncException("Cannot be null");
        }
        this.delegate = delegate;
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
}
