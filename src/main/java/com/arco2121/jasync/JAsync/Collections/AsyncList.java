package com.arco2121.jasync.JAsync.Collections;

import com.arco2121.jasync.Types.Interfaces.AsyncCollection;
import com.arco2121.jasync.JAsync.Async;
import com.arco2121.jasync.JAsync.Running.Asyncable;
import com.arco2121.jasync.Types.Interfaces.JSONable;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.arco2121.jasync.JAsync.Async.timeout;

public final class AsyncList<T> extends AbstractList<T> implements RandomAccess, JSONable, Cloneable, AsyncCollection {

    @Serial
    private static final long serialVersionUID = 1L;
    private final List<T> storage = new CopyOnWriteArrayList<>();
    private volatile boolean completed = false;
    private final List<Runnable> completionListeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public boolean add(T item) {
        if (!completed) {
            return storage.add(item);
        }
        return false;
    }

    public synchronized void complete() {
        if (completed) return;
        completed = true;
        synchronized (completionListeners) {
            completionListeners.forEach(Runnable::run);
            completionListeners.clear();
        }
    }

    public void onComplete(Runnable callback) {
        if (completed) callback.run();
        else completionListeners.add(callback);
    }

    @Override
    public T get(int index) {
        return storage.get(index);
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("AsyncList is read-only from outside");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("AsyncList is read-only from outside");
    }

    @Override
    public Stream<T> stream() {
        return storage.stream();
    }

    @Override
    public Asyncable<T[]> awaitToArray(Function<Integer, ?> generator) {
        return this.awaitToList().then(list -> list.toArray((T[]) generator.apply(list.size())));
    }

    @Override
    public Asyncable<List<?>> awaitToList() {
        return Async.async(() -> {
            while (!completed) {
                timeout(10);
            }
            return new ArrayList<>(storage);
        });
    }

    public AsyncArray deType() {
        AsyncArray k = new AsyncArray();
        k.addAll(this);
        return k;
    }

    public Asyncable<T> awaitGet(int index) {
        return Async.async(() -> {
            while (storage.size() <= index && !completed) {
                timeout(10);
            }
            return storage.get(index);
        });
    }

    public Asyncable<Integer> awaitSize() {
        return Async.async(() -> {
            while (!completed) {
                timeout(10);
            }
            return storage.size();
        });
    }

    @Override
    public Object clone() {
        AsyncList<T> copy = new AsyncList<>();
        for (T item : this.storage) copy.add(item);
        if (this.completed) copy.complete();
        return copy;
    }

    @Serial
    private Object writeReplace() {
        return new ArrayList<>(this.storage);
    }

    @Override
    public Iterator<?> asyncIterator() {
        return this.storage.iterator();
    }
}