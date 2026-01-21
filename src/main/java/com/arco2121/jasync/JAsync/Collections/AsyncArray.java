package com.arco2121.jasync.JAsync.Collections;

import com.arco2121.jasync.Types.Interfaces.AsyncCollection;
import com.arco2121.jasync.JAsync.Async;
import com.arco2121.jasync.JAsync.Running.Asyncable;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.arco2121.jasync.JAsync.Async.timeout;

public class AsyncArray extends AbstractList<Object> implements RandomAccess, Serializable, Cloneable, AsyncCollection {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<Object> storage = new ArrayList<>();
    private volatile boolean completed = false;
    private final List<Runnable> completionListeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public boolean add(Object item) {
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
    public Object get(int index) {
        return storage.get(index);
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public Object remove(int index) {
        throw new UnsupportedOperationException("AsyncList is read-only from outside");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("AsyncList is read-only from outside");
    }

    @Override
    public Stream<Object> stream() {
        return storage.stream();
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

    @Override
    public Asyncable<?> awaitToArray(Function<Integer, ?> generator) {
        return this.awaitToList().then(list -> list.toArray((Object[]) generator.apply(list.size())));
    }

    public Asyncable<Object> awaitGet(int index) {
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
        AsyncArray copy = new AsyncArray();
        for (Object item : this.storage) copy.add(item);
        if (this.completed) copy.complete();
        return copy;
    }

    @Serial
    private Object writeReplace() {
        return new ArrayList<>(this.storage);
    }
}
