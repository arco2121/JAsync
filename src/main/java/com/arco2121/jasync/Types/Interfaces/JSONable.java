package com.arco2121.jasync.Types.Interfaces;

import com.arco2121.jasync.Types.Exceptions.CannotConstructJSONableException;
import com.arco2121.jasync.Types.Exceptions.CannotDeconstructJSONableException;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public interface JSONable extends Externalizable {

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
        skipWhitespace(src, pos);
        char c = peek(src, pos);
        if (c == '{') return parseJSONMap(src, pos);
        if (c == '[') return parseJSONList(src, pos);
        if (c == '"') return parseJSONString(src, pos);
        if (c == 't') { consume(src, pos, "true"); return true; }
        if (c == 'f') { consume(src, pos, "false"); return false; }
        if (c == 'n') { consume(src, pos, "null"); return null; }
        return parseJSONNumber(src, pos);
    }

    private static Map<String, Object> parseJSONMap(String src, int[] pos) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        consume(src, pos, '{');
        skipWhitespace(src, pos);
        if (peek(src, pos) == '}') { consume(src, pos, '}'); return map; }
        while (true) {
            skipWhitespace(src, pos);
            String key = parseJSONString(src, pos);
            skipWhitespace(src, pos);
            consume(src, pos, ':');
            map.put(key, parseJSONValue(src, pos));
            skipWhitespace(src, pos);
            char next = peek(src, pos);
            if (next == '}') { consume(src, pos, '}'); break; }
            consume(src, pos, ',');
        }
        return map;
    }

    private static List<Object> parseJSONList(String src, int[] pos) throws Exception {
        List<Object> list = new ArrayList<>();
        consume(src, pos, '[');
        skipWhitespace(src, pos);
        if (peek(src, pos) == ']') { consume(src, pos, ']'); return list; }
        while (true) {
            list.add(parseJSONValue(src, pos));
            skipWhitespace(src, pos);
            char next = peek(src, pos);
            if (next == ']') { consume(src, pos, ']'); break; }
            consume(src, pos, ',');
        }
        return list;
    }

    private static String parseJSONString(String src, int[] pos) throws Exception {
        consume(src, pos, '"');
        StringBuilder sb = new StringBuilder();
        while (pos[0] < src.length()) {
            char c = src.charAt(pos[0]++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char next = src.charAt(pos[0]++);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(next);
                }
            } else sb.append(c);
        }
        throw new Exception("Unterminated string");
    }

    private static Number parseJSONNumber(String src, int[] pos) {
        int start = pos[0];
        while (pos[0] < src.length() && "-0123456789.eE+-".indexOf(src.charAt(pos[0])) != -1) pos[0]++;
        String n = src.substring(start, pos[0]);
        return (n.contains(".") || n.toLowerCase().contains("e")) ? Double.parseDouble(n) : Long.parseLong(n);
    }

    private static void skipWhitespace(String src, int[] pos) {
        while (pos[0] < src.length() && src.charAt(pos[0]) <= ' ') pos[0]++;
    }

    private static char peek(String src, int[] pos) {
        return pos[0] < src.length() ? src.charAt(pos[0]) : 0;
    }

    private static void consume(String src, int[] pos, char expected) throws Exception {
        if (peek(src, pos) == expected) pos[0]++;
        else throw new Exception("Expected " + expected + " at pos " + pos[0]);
    }

    private static void consume(String src, int[] pos, String expected) throws Exception {
        if (src.startsWith(expected, pos[0])) pos[0] += expected.length();
        else throw new Exception("Expected " + expected + " at pos " + pos[0]);
    }

    private static String escapeJSONString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    private static String constructJSONInternal(Object obj, Set<Object> visited) throws CannotConstructJSONableException {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escapeJSONString((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj.getClass().isEnum()) return "\"" + ((Enum<?>) obj).name() + "\"";

        if (!visited.add(obj)) {
            throw new CannotConstructJSONableException("Circular reference: " + obj.getClass().getName());
        }

        try {
            if (obj instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) obj).entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<?, ?> e = it.next();
                    sb.append("\"").append(escapeJSONString(String.valueOf(e.getKey()))).append("\":")
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

    private static String constructFromFieldsInternal(Object obj, Set<Object> visited) throws CannotConstructJSONableException {
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
                    sb.append("\"").append(escapeJSONString(f.getName())).append("\":")
                            .append(constructJSONInternal(f.get(obj), visited));
                    first = false;
                } catch (IllegalAccessException e) {
                    throw new CannotConstructJSONableException("Field access error: " + f.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return sb.append("}").toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> T convert(Object val, Class<T> toClass, Type genType) throws Exception {
        if (val == null) return null;

        if (toClass.isEnum()) {
            String name = val.toString();
            for (T c : toClass.getEnumConstants()) {
                if (((Enum<?>) c).name().equalsIgnoreCase(name)) return c;
            }
            throw new Exception("Unknown enum constant: " + name);
        }

        if (toClass.isInstance(val)) return (T) val;
        if (toClass == int.class || toClass == Integer.class) return (T) Integer.valueOf(val.toString());
        if (toClass == long.class || toClass == Long.class) return (T) Long.valueOf(val.toString());
        if (toClass == double.class || toClass == Double.class) return (T) Double.valueOf(val.toString());
        if (toClass == boolean.class || toClass == Boolean.class) return (T) Boolean.valueOf(val.toString());

        if (toClass.isArray() && val instanceof List<?> list) {
            Class<?> componentType = toClass.getComponentType();
            Object array = Array.newInstance(componentType, list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, convert(list.get(i), componentType, componentType));
            }
            return (T) array;
        }

        if (Collection.class.isAssignableFrom(toClass) && val instanceof List) {
            Collection<Object> coll = toClass.isInterface() ? (Set.class.isAssignableFrom(toClass) ? new HashSet<>() : new ArrayList<>()) : (Collection<Object>) toClass.getDeclaredConstructor().newInstance();
            Type cType = (genType instanceof ParameterizedType) ? ((ParameterizedType) genType).getActualTypeArguments()[0] : Object.class;
            Class<?> cClass = (cType instanceof Class) ? (Class<?>) cType : (Class<?>) ((ParameterizedType) cType).getRawType();
            for (Object item : (List<?>) val) {
                coll.add(convert(item, (Class<Object>) cClass, cType));
            }
            return (T) coll;
        }

        if (val instanceof Map) {
            if (Map.class.isAssignableFrom(toClass)) return (T) val;
            T instance;
            try {
                Constructor<T> ctor = toClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                instance = ctor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new Exception("No-args constructor missing for " + toClass.getSimpleName());
            }
            Map<String, Object> map = (Map<String, Object>) val;
            Class<?> current = toClass;
            while (current != null && current != Object.class) {
                for (Field f : current.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers()) || f.isSynthetic()) continue;
                    if (map.containsKey(f.getName())) {
                        f.setAccessible(true);
                        f.set(instance, convert(map.get(f.getName()), f.getType(), f.getGenericType()));
                    }
                }
                current = current.getSuperclass();
            }
            return instance;
        }
        return (T) val;
    }

    static <T> T deconstructJSON(String json, Class<T> toClass) throws CannotDeconstructJSONableException {
        try {
            if (json == null || json.trim().isEmpty()) return null;
            int[] pos = new int[]{0};
            Object rawData = parseJSONValue(json.trim(), pos);
            return convert(rawData, toClass, toClass);
        } catch (Exception e) {
            throw new CannotDeconstructJSONableException(e.getMessage());
        }
    }

    static String constructJSON(Object obj) throws CannotConstructJSONableException {
        return constructJSONInternal(obj, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * Overridable method to create a JSON, the super() is generic constructJSON() in JSON
     * @return String
     * @throws CannotConstructJSONableException
     */
    default String toJSON() throws CannotConstructJSONableException {
        return constructJSON(this);
    }

    /**
     * Overridable method to create object, the super() is generic deconstructJSON() in JSON
     * @param json
     * @param toClass
     * @return T
     * @throws CannotDeconstructJSONableException
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    static <T> T fromJSON(String json, Class<T> toClass) throws CannotDeconstructJSONableException {
        if (toClass.isInterface())
            return deconstructJSON(json, toClass);
        try {
            Method method = toClass.getDeclaredMethod("fromJSON", String.class);
            if (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() == toClass) {
                method.setAccessible(true);
                return (T) method.invoke(null, json);
            }
        } catch (NoSuchMethodException ignored) { } catch (Exception e) {
            throw new CannotDeconstructJSONableException("Error invoking fromJSON on " + toClass.getName() + ": " + e.getMessage());
        }
        return deconstructJSON(json, toClass);
    }

    /**
     * Overridable method that represent the standard Java Serialization Writing process
     * @param out the stream to write the object to
     * @throws IOException
     * @throws CannotConstructJSONableException
     */
    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        try {
            byte[] bytes = toJSON().getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
        } catch (CannotConstructJSONableException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Overridable method that represent the standard Java Serialization Reading process
     * @param in the stream to read data from in order to restore the object
     * @throws IOException
     * @throws CannotDeconstructJSONableException
     */
    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        try {
            int length = in.readInt();
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            String json = new String(bytes, StandardCharsets.UTF_8);
            Object decoded = deconstructJSON(json, this.getClass());
            copyFrom(decoded);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }
}