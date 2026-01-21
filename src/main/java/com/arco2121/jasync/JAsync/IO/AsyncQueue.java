package com.arco2121.jasync.JAsync.IO;

import com.arco2121.jasync.Types.Interfaces.AsyncCollection;
import com.arco2121.jasync.JAsync.Async;
import com.arco2121.jasync.JAsync.Running.Asyncable;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.List;

public final class AsyncQueue<T> implements AsyncCollection {

    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private final Object CLOSE = new Object();
    private volatile boolean closed = false;
    private final List<Runnable> closeListeners = new ArrayList<>();

    public void add(T item) {
        if (!closed && item != null) queue.offer(item);
    }

    public synchronized void close() {
        if (closed) return;
        closed = true;
        queue.offer((T) CLOSE);
        closeListeners.forEach(Runnable::run);
    }

    public synchronized void onClose(Runnable callback) {
        if (closed) callback.run();
        else closeListeners.add(callback);
    }

    public <R> AsyncQueue<R> map(Function<T, R> mapper) {
        AsyncQueue<R> newQueue = new AsyncQueue<>();
        this.forEach(item -> newQueue.add(mapper.apply(item)));
        this.onClose(newQueue::close);
        return newQueue;
    }

    public AsyncQueue<T> filter(Predicate<T> condition) {
        AsyncQueue<T> newQueue = new AsyncQueue<>();
        this.forEach(item -> {
            if (condition.test(item)) newQueue.add(item);
        });
        this.onClose(newQueue::close);
        return newQueue;
    }

    @Override
    public Asyncable<List<?>> awaitToList() {
        return Async.async(() -> {
            List<T> list = new ArrayList<>();
            while (true) {
                T item = queue.take();
                if (item == CLOSE) {
                    queue.offer((T) CLOSE);
                    break;
                }
                list.add(item);
            }
            return list;
        });
    }

    public void forEach(Consumer<T> action) {
        Async.async(() -> {
            try {
                while (true) {
                    T item = queue.take();
                    if (item == CLOSE) {
                        queue.offer((T)CLOSE);
                        break;
                    }
                    action.accept(item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
    }

    @Override
    public Asyncable<T[]> awaitToArray(Function<Integer, ?> sizer) {
        return this.awaitToList().then(list -> list.toArray((T[]) sizer.apply(list.size())));
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int partialSize() {
        return queue.size();
    }
}