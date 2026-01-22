package com.arco2121.jasync.JAsync.Collections;

import com.arco2121.jasync.Types.Exceptions.CannotConstructJSONableException;
import com.arco2121.jasync.Types.Exceptions.CannotDeconstructJSONableException;
import com.arco2121.jasync.Types.Interfaces.AsyncCollection;
import com.arco2121.jasync.Types.Interfaces.JSONable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public final class JSON implements JSONable, Map<String, Object> {

    private LinkedHashMap<String, Object> data;

    public JSON() {
        this.data = new LinkedHashMap<>();
    }
    private JSON(LinkedHashMap<String, Object> data) {
        this.data = data;
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
    public Object get(String... embeded) {
        Object current = this;
        for (String key : embeded) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }
        return current;
    }
    @Override
    public JSON put(String key, Object value) {
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
    public final JSON putAll(Map<String, Object>... data) {
        for(Map<String, Object> map : data) {
            this.data.putAll(map);
        }
        return this;
    }
    @SafeVarargs
    public final JSON putAll(Map.Entry<String, Object>... data) {
        for(Map.Entry<String, Object> map : data) {
            this.data.put(map.getKey(), map.getValue());
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void copyFrom(Object other) {
        if (other instanceof JSON) {
            this.data = new LinkedHashMap<>(((JSON) other).data);
        } else if (other instanceof Map) {
            this.data = new LinkedHashMap<>((Map<String, Object>) other);
        }
    }

    @Override
    public String toJSON() throws CannotConstructJSONableException {
        return JSONable.constructJSON(data);
    }

    public static JSON fromJSON(String json) throws CannotDeconstructJSONableException {
        return new JSON(JSONable.deconstructJSON(json, LinkedHashMap.class));
    }
}
