package com.arco2121.jasync.Types.Interfaces.ObjectNotations;

import com.arco2121.jasync.Types.Exceptions.CannotConstructONException;
import com.arco2121.jasync.Types.Exceptions.CannotDeconstructONException;
import com.arco2121.jasync.Types.Interfaces.ObjectNotation;

import java.lang.reflect.*;
import java.util.*;

public interface TOONable extends ObjectNotation {

    @Override
    default void copyFrom(Object other) {
        if (other == null) return;
        for (Field f : ObjectNotation.getSerializableFields(this.getClass())) {
            try { f.set(this, f.get(other)); } catch (Exception ignored) {}
        }
    }

    private static Object parseTOONValue(String src, int[] pos, int level) throws Exception {
        ObjectNotation.skipWhitespace(src, pos);
        char c = ObjectNotation.peek(src, pos);
        if (c == '"') return ObjectNotation.parseString(src, pos);
        if (c == '[') return parseTable(src, pos, level);
        if (Character.isDigit(c) || c == '-') return ObjectNotation.parseNumber(src, pos);
        if (src.startsWith("true", pos[0])) { pos[0]+=4; return true; }
        if (src.startsWith("false", pos[0])) { pos[0]+=5; return false; }
        if (src.startsWith("null", pos[0])) { pos[0]+=4; return null; }
        return parseTOONObject(src, pos, level);
    }

    private static Map<String, Object> parseTOONObject(String src, int[] pos, int level) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        while (pos[0] < src.length()) {
            int startLine = pos[0];
            int indent = countIndent(src, pos);
            if (indent < level && !map.isEmpty()) { pos[0] = startLine; break; }

            int endKey = src.indexOf(':', pos[0]);
            if (endKey == -1) break;
            String key = src.substring(pos[0], endKey).trim();
            pos[0] = endKey + 1;

            ObjectNotation.skipWhitespace(src, pos);
            map.put(key, parseTOONValue(src, pos, indent + 1));
        }
        return map;
    }

    private static List<Object> parseTable(String src, int[] pos, int level) throws Exception {
        ObjectNotation.consume(src, pos, '[');
        while (pos[0] < src.length() && src.charAt(pos[0]) != ']') pos[0]++;
        ObjectNotation.consume(src, pos, ']');
        ObjectNotation.consume(src, pos, '{');
        int endCols = src.indexOf('}', pos[0]);
        String[] cols = src.substring(pos[0], endCols).split(",");
        pos[0] = endCols + 1;
        ObjectNotation.consume(src, pos, ':');

        List<Object> list = new ArrayList<>();
        while (pos[0] < src.length()) {
            int startLine = pos[0];
            if (countIndent(src, pos) <= level) { pos[0] = startLine; break; }
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < cols.length; i++) {
                row.put(cols[i].trim(), parseTOONValue(src, pos, 0));
                if (i < cols.length - 1 && ObjectNotation.peek(src, pos) == ',') pos[0]++;
            }
            list.add(row);
        }
        return list;
    }

    private static String constructTOONInternal(Object obj, int indent, Set<Object> visited) throws Exception {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + ObjectNotation.escapeString((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();

        if (obj instanceof Collection<?> col) {
            if (col.isEmpty()) return "[]";
            Object first = col.iterator().next();
            if (first instanceof String || first instanceof Number || first instanceof Boolean) return col.toString();

            List<Field> fields = ObjectNotation.getSerializableFields(first.getClass());
            StringBuilder sb = new StringBuilder("[" + col.size() + "]{");
            for (int i = 0; i < fields.size(); i++) sb.append(fields.get(i).getName()).append(i == fields.size()-1 ? "" : ",");
            sb.append("}:");
            for (Object item : col) {
                sb.append("\n").append("  ".repeat(indent + 1));
                for (int i = 0; i < fields.size(); i++) {
                    sb.append(constructTOONInternal(fields.get(i).get(item), 0, visited)).append(i == fields.size()-1 ? "" : ", ");
                }
            }
            return sb.toString();
        }

        if (!visited.add(obj)) throw new CannotConstructONException("Circular reference");
        StringBuilder sb = new StringBuilder();
        String tabs = "  ".repeat(indent);
        for (Field f : ObjectNotation.getSerializableFields(obj.getClass())) {
            sb.append("\n").append(tabs).append(f.getName()).append(": ").append(constructTOONInternal(f.get(obj), indent + 1, visited));
        }
        visited.remove(obj);
        return sb.toString();
    }

    private static int countIndent(String src, int[] pos) {
        int start = pos[0];
        while (pos[0] < src.length() && src.charAt(pos[0]) == ' ') pos[0]++;
        return (pos[0] - start) / 2;
    }

    static <T> T deconstruct(String toon, Class<T> toClass) throws CannotDeconstructONException {
        try {
            int[] pos = {0};
            return ObjectNotation.convert(parseTOONValue(toon, pos, 0), toClass, toClass);
        } catch (Exception e) { throw new CannotDeconstructONException(e.getMessage()); }
    }

    @Override
    default String construct(Object obj) throws CannotConstructONException {
        try {
            return constructTOONInternal(obj, 0, Collections.newSetFromMap(new IdentityHashMap<>())).trim();
        } catch (Exception e) {
            throw new CannotConstructONException(e.getMessage());
        }
    }

    /**
     * Overridable method to create a TOON, the super() is generic construct() in TOONable
     * @return String
     * @throws CannotConstructONException
     */
    @Override
    default String toNotation() throws CannotConstructONException {
        try { return constructTOONInternal(this, 0, Collections.newSetFromMap(new IdentityHashMap<>())).trim(); }
        catch (Exception e) { throw new CannotConstructONException(e.getMessage()); }
    }

    /**
     * Overridable method to create object, the super() is generic deconstruct() in TOONNable
     * @param toon
     * @param toClass
     * @return T
     * @throws CannotDeconstructONException
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    static <T> T fromNotation(String toon, Class<T> toClass) throws CannotDeconstructONException {
        if (toClass.isInterface()) return deconstruct(toon, toClass);
        try {
            Method method = toClass.getDeclaredMethod("fromNotation", String.class);
            if (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() == toClass) {
                method.setAccessible(true);
                return (T) method.invoke(null, toon);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new CannotDeconstructONException("Errore in fromNotation (TOON): " + e.getMessage());
        }
        return deconstruct(toon, toClass);
    }
}