package com.arco2121.jasync.Types.Interfaces;

import com.arco2121.jasync.Types.Exceptions.CannotConstructONException;
import com.arco2121.jasync.Types.Exceptions.CannotDeconstructONException;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public interface ObjectNotation extends Externalizable {

    void copyFrom(Object other);

    @SuppressWarnings("unchecked")
    static <T> T convert(Object val, Class<T> toClass, Type genType) throws Exception {
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

    static <T> T deconstruct(String json, Class<T> toClass) throws CannotDeconstructONException {
        return null;
    }

    default String construct(Object obj) throws CannotConstructONException {
        return null;
    }

    String toNotation() throws CannotConstructONException;

    static <T> T fromNotation(String json, Class<T> toClass) throws CannotDeconstructONException {
        return null;
    }

    static String parseString(String src, int[] pos) throws Exception {
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

    static Number parseNumber(String src, int[] pos) {
        int start = pos[0];
        while (pos[0] < src.length() && "-0123456789.eE+-".indexOf(src.charAt(pos[0])) != -1) pos[0]++;
        String n = src.substring(start, pos[0]);
        return (n.contains(".") || n.toLowerCase().contains("e")) ? Double.parseDouble(n) : Long.parseLong(n);
    }

    static String escapeString(String s) {
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

    static void consume(String src, int[] pos, char expected) throws Exception {
        if (peek(src, pos) == expected) pos[0]++;
        else throw new Exception("Expected " + expected + " at pos " + pos[0]);
    }

    static void consume(String src, int[] pos, String expected) throws Exception {
        if (src.startsWith(expected, pos[0])) pos[0] += expected.length();
        else throw new Exception("Expected " + expected + " at pos " + pos[0]);
    }

    static char peek(String src, int[] pos) {
        return pos[0] < src.length() ? src.charAt(pos[0]) : 0;
    }

    static List<Field> getSerializableFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic()) continue;
                f.setAccessible(true);
                fields.add(f);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    static void skipWhitespace(String src, int[] pos) {
        while (pos[0] < src.length() && src.charAt(pos[0]) <= ' ') pos[0]++;
    }

    /**
     * Overridable method that represent the standard Java Serialization Writing process
     * @param out the stream to write the object to
     * @throws IOException
     * @throws CannotConstructONException
     */
    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        try {
            byte[] bytes = toNotation().getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
        } catch (CannotConstructONException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Overridable method that represent the standard Java Serialization Reading process
     * @param in the stream to read data from in order to restore the object
     * @throws IOException
     * @throws CannotDeconstructONException
     */
    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        try {
            int length = in.readInt();
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            String json = new String(bytes, StandardCharsets.UTF_8);
            Object decoded = deconstruct(json, this.getClass());
            copyFrom(decoded);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }
}