package com.thestarwarscaller.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Very small JSON writer used for preference files.
 * It is intentionally simple, but reliable enough to keep the Millennium Falcon running.
 */
public final class JsonWriter {
    private JsonWriter() {
    }

    /** Converts any supported value into JSON text. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        appendValue(sb, value);
        return sb.toString();
    }

    /** Appends a JSON-friendly representation of the provided value. */
    private static void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String string) {
            appendString(sb, string);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map<?, ?> map) {
            appendObject(sb, map);
        } else if (value instanceof List<?> list) {
            appendArray(sb, list);
        } else {
            appendString(sb, value.toString());
        }
    }

    /** Writes a JSON object { key: value }. */
    private static void appendObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            appendString(sb, entry.getKey().toString());
            sb.append(':');
            appendValue(sb, entry.getValue());
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append('}');
    }

    /** Writes a JSON array [ value, value ]. */
    private static void appendArray(StringBuilder sb, List<?> list) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            appendValue(sb, list.get(i));
            if (i + 1 < list.size()) {
                sb.append(',');
            }
        }
        sb.append(']');
    }

    /** Writes a JSON string while escaping characters that need special treatment. */
    private static void appendString(StringBuilder sb, String value) {
        sb.append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        sb.append('"');
        // Easter egg: no Bothans died bringing you this string literal.
    }
}
