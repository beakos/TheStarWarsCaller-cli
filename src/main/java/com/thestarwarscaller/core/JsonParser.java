package com.thestarwarscaller.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal JSON parser so we can keep dependencies light.
 * Built specifically for this project; it understands objects, arrays, numbers, and strings.
 * Consider it a patient protocol droid that translates plain text into Java structures.
 */
public final class JsonParser {
    /** Entire JSON input as a single string. */
    private final String input;
    /** Current position while we walk through the characters. */
    private int index;

    private JsonParser(String input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    /** Entry point used by other classes to parse text into Java objects. */
    public static Object parse(String json) {
        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("Unexpected trailing characters in JSON");
        }
        return value;
    }

    /** @return true when we have read every character. */
    private boolean isAtEnd() {
        return index >= input.length();
    }

    /** Moves past any whitespace so the next call looks at useful data. */
    private void skipWhitespace() {
        while (!isAtEnd()) {
            char c = input.charAt(index);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                index++;
            } else {
                break;
            }
        }
    }

    /** Parses whichever value appears next (object, array, string, number, etc.). */
    private Object parseValue() {
        if (isAtEnd()) {
            throw new IllegalArgumentException("Unexpected end of input");
        }
        char c = input.charAt(index);
        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 't':
            case 'f':
                return parseBoolean();
            case 'n':
                return parseNull();
            default:
                if (c == '-' || Character.isDigit(c)) {
                    return parseNumber();
                }
                throw new IllegalArgumentException("Unexpected character in JSON: " + c);
        }
    }

    /** Parses a JSON object: { "key": value, ... }. */
    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (peek('}')) {
            expect('}');
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            if (peek(',')) {
                expect(',');
                continue;
            }
            expect('}');
            break;
        }
        return map;
    }

    /** Parses a JSON array: [ value, value, ... ]. */
    private List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (peek(']')) {
            expect(']');
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(parseValue());
            skipWhitespace();
            if (peek(',')) {
                expect(',');
                continue;
            }
            expect(']');
            break;
        }
        return list;
    }

    /** Parses a JSON string while handling escape sequences. */
    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd()) {
            char c = input.charAt(index++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (isAtEnd()) {
                    throw new IllegalArgumentException("Incomplete escape sequence");
                }
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        sb.append(parseUnicode());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown escape sequence: \\" + escaped);
                }
            } else {
                sb.append(c);
            }
        }
        throw new IllegalArgumentException("Unterminated string literal");
    }

    /** Converts a four-digit hexadecimal escape into a single character. */
    private char parseUnicode() {
        if (index + 4 > input.length()) {
            throw new IllegalArgumentException("Incomplete unicode escape");
        }
        String hex = input.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid unicode escape: \\u" + hex);
        }
    }

    /**
     * Parses a number token (integer or decimal, optionally with exponent).
     * Not meant to be the fastest solution, but more than enough for our dataset.
     */
    private Number parseNumber() {
        int start = index;
        if (input.charAt(index) == '-') {
            index++;
        }
        while (!isAtEnd() && Character.isDigit(input.charAt(index))) {
            index++;
        }
        if (!isAtEnd() && input.charAt(index) == '.') {
            index++;
            while (!isAtEnd() && Character.isDigit(input.charAt(index))) {
                index++;
            }
        }
        if (!isAtEnd() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
            index++;
            if (!isAtEnd() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                index++;
            }
            while (!isAtEnd() && Character.isDigit(input.charAt(index))) {
                index++;
            }
        }
        String literal = input.substring(start, index);
        if (literal.contains(".") || literal.contains("e") || literal.contains("E")) {
            return Double.parseDouble(literal);
        }
        long asLong = Long.parseLong(literal);
        if (asLong <= Integer.MAX_VALUE && asLong >= Integer.MIN_VALUE) {
            return (int) asLong;
        }
        return asLong;
    }

    /** Parses a boolean literal (true/false). */
    private Boolean parseBoolean() {
        if (match("true")) {
            return Boolean.TRUE;
        }
        if (match("false")) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid boolean literal");
    }

    /** Parses the null literal. */
    private Object parseNull() {
        if (match("null")) {
            return null;
        }
        throw new IllegalArgumentException("Invalid null literal");
    }

    /** Tries to match the provided string at the current index. */
    private boolean match(String literal) {
        if (input.regionMatches(index, literal, 0, literal.length())) {
            index += literal.length();
            return true;
        }
        return false;
    }

    /** Checks the next character without consuming it. */
    private boolean peek(char expected) {
        return !isAtEnd() && input.charAt(index) == expected;
    }

    /** Ensures the next character matches what we expect, otherwise throws. */
    private void expect(char expected) {
        if (isAtEnd() || input.charAt(index) != expected) {
            throw new IllegalArgumentException("Expected '" + expected + "' in JSON");
        }
        index++;
        // Easter egg: when this method fails, even Master Yoda raises an eyebrow.
    }
}
