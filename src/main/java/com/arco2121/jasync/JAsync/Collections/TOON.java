package com.arco2121.jasync.JAsync.Collections;

import com.arco2121.jasync.Types.Exceptions.CannotConstructONException;
import com.arco2121.jasync.Types.Exceptions.CannotDeconstructONException;
import com.arco2121.jasync.Types.Interfaces.ObjectNotations.TOONable;

import java.util.*;

public final class TOON implements TOONable, Map<String, Object> {

    private LinkedHashMap<String, Object> data;

    public TOON() {
        this.data = new LinkedHashMap<>();
    }

    private TOON(LinkedHashMap<String, Object> data) {
        this.data = data != null ? data : new LinkedHashMap<>();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return data.get(key);
    }

    public Object get(String... embedded) {
        Object current = this;
        for (String key : embedded) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }
        return current;
    }

    @Override
    public TOON put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @Override
    public Object remove(Object key) {
        return data.remove(key);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @Override
    public Collection<Object> values() {
        return data.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return data.entrySet();
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        data.putAll(m);
    }

    @SafeVarargs
    public final TOON putAll(Map<String, Object>... maps) {
        for (Map<String, Object> map : maps) {
            this.data.putAll(map);
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void copyFrom(Object other) {
        if (other instanceof TOON) {
            this.data = new LinkedHashMap<>(((TOON) other).data);
        } else if (other instanceof Map) {
            this.data = new LinkedHashMap<>((Map<String, Object>) other);
        }
    }

    @Override
    public String toNotation() throws CannotConstructONException {
        return this.construct(data);
    }

    @SuppressWarnings("unchecked")
    public static TOON fromNotation(String toon) throws CannotDeconstructONException {
        return new TOON(TOONable.deconstruct(toon, LinkedHashMap.class));
    }

    @Override
    public String toString() {
        try {
            return toNotation();
        } catch (Exception e) {
            return data.toString();
        }
    }
}