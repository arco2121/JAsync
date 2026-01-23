package com.arco2121.jasync.Types.Interfaces.ObjectNotations;

import com.arco2121.jasync.Types.Exceptions.CannotConstructONException;
import com.arco2121.jasync.Types.Exceptions.CannotDeconstructONException;
import com.arco2121.jasync.Types.Interfaces.ObjectNotation;

import java.lang.reflect.*;
import java.util.*;

public interface JSONable extends ObjectNotation {

    @Override
    default void copyFrom(Object other) {
        if (other == null) return;
        Class<?> clazz = this.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    f.set(this, f.get(other));
                } catch (Exception ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static Object parseJSONValue(String src, int[] pos) throws Exception {
        ObjectNotation.skipWhitespace(src, pos);
        char c = ObjectNotation.peek(src, pos);
        if (c == '{') return parseJSONMap(src, pos);
        if (c == '[') return parseJSONList(src, pos);
        if (c == '"') return ObjectNotation.parseString(src, pos);
        if (c == 't') { ObjectNotation.consume(src, pos, "true"); return true; }
        if (c == 'f') { ObjectNotation.consume(src, pos, "false"); return false; }
        if (c == 'n') { ObjectNotation.consume(src, pos, "null"); return null; }
        return ObjectNotation.parseNumber(src, pos);
    }

    private static Map<String, Object> parseJSONMap(String src, int[] pos) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        ObjectNotation.consume(src, pos, '{');
        ObjectNotation.skipWhitespace(src, pos);
        if (ObjectNotation.peek(src, pos) == '}') { ObjectNotation.consume(src, pos, '}'); return map; }
        while (true) {
            ObjectNotation.skipWhitespace(src, pos);
            String key = ObjectNotation.parseString(src, pos);
            ObjectNotation.skipWhitespace(src, pos);
            ObjectNotation.consume(src, pos, ':');
            map.put(key, parseJSONValue(src, pos));
            ObjectNotation.skipWhitespace(src, pos);
            char next = ObjectNotation.peek(src, pos);
            if (next == '}') { ObjectNotation.consume(src, pos, '}'); break; }
            ObjectNotation.consume(src, pos, ',');
        }
        return map;
    }

    private static List<Object> parseJSONList(String src, int[] pos) throws Exception {
        List<Object> list = new ArrayList<>();
        ObjectNotation.consume(src, pos, '[');
        ObjectNotation.skipWhitespace(src, pos);
        if (ObjectNotation.peek(src, pos) == ']') { ObjectNotation.consume(src, pos, ']'); return list; }
        while (true) {
            list.add(parseJSONValue(src, pos));
            ObjectNotation.skipWhitespace(src, pos);
            char next = ObjectNotation.peek(src, pos);
            if (next == ']') { ObjectNotation.consume(src, pos, ']'); break; }
            ObjectNotation.consume(src, pos, ',');
        }
        return list;
    }

    private static String constructJSONInternal(Object obj, Set<Object> visited) throws CannotConstructONException {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + ObjectNotation.escapeString((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj.getClass().isEnum()) return "\"" + ((Enum<?>) obj).name() + "\"";

        if (!visited.add(obj)) {
            throw new CannotConstructONException("Circular reference: " + obj.getClass().getName());
        }

        try {
            if (obj instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) obj).entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<?, ?> e = it.next();
                    sb.append("\"").append(ObjectNotation.escapeString(String.valueOf(e.getKey()))).append("\":")
                            .append(constructJSONInternal(e.getValue(), visited));
                    if (it.hasNext()) sb.append(",");
                }
                return sb.append("}").toString();
            }
            if (obj instanceof Collection) {
                StringBuilder sb = new StringBuilder("[");
                Iterator<?> it = ((Collection<?>) obj).iterator();
                while (it.hasNext()) {
                    sb.append(constructJSONInternal(it.next(), visited));
                    if (it.hasNext()) sb.append(",");
                }
                return sb.append("]").toString();
            }
            return constructFromFieldsInternal(obj, visited);
        } finally {
            visited.remove(obj);
        }
    }

    private static String constructFromFieldsInternal(Object obj, Set<Object> visited) throws CannotConstructONException {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic()) continue;
                if (!first) sb.append(",");
                f.setAccessible(true);
                try {
                    sb.append("\"").append(ObjectNotation.escapeString(f.getName())).append("\":")
                            .append(constructJSONInternal(f.get(obj), visited));
                    first = false;
                } catch (IllegalAccessException e) {
                    throw new CannotConstructONException("Field access error: " + f.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return sb.append("}").toString();
    }

    static <T> T deconstruct(String json, Class<T> toClass) throws CannotDeconstructONException {
        try {
            if (json == null || json.trim().isEmpty()) return null;
            int[] pos = new int[]{0};
            Object rawData = parseJSONValue(json.trim(), pos);
            return ObjectNotation.convert(rawData, toClass, toClass);
        } catch (Exception e) {
            throw new CannotDeconstructONException(e.getMessage());
        }
    }

    @Override
    default String construct(Object obj) throws CannotConstructONException {
        return constructJSONInternal(obj, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * Overridable method to create a JSON, the super() is generic construct() in JSONable
     * @return String
     * @throws CannotConstructONException
     */
    @Override
    default String toNotation() throws CannotConstructONException {
        return construct(this);
    }

    /**
     * Overridable method to create object, the super() is generic deconstruct() in JSONable
     * @param json
     * @param toClass
     * @return T
     * @throws CannotDeconstructONException
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    static <T> T fromNotation(String json, Class<T> toClass) throws CannotDeconstructONException {
        if (toClass.isInterface())
            return deconstruct(json, toClass);
        try {
            Method method = toClass.getDeclaredMethod("fromNotation", String.class);
            if (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() == toClass) {
                method.setAccessible(true);
                return (T) method.invoke(null, json);
            }
        } catch (NoSuchMethodException ignored) { } catch (Exception e) {
            throw new CannotDeconstructONException("Error invoking fromJSON on " + toClass.getName() + ": " + e.getMessage());
        }
        return deconstruct(json, toClass);
    }
}