package com.kaoshi.exam;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

final class ExamRowValues {
    private ExamRowValues() {
    }

    static Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        String upperSnake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        if (row.containsKey(upperSnake)) {
            return row.get(upperSnake);
        }
        String lowerSnake = upperSnake.toLowerCase();
        if (row.containsKey(lowerSnake)) {
            return row.get(lowerSnake);
        }
        String upper = key.toUpperCase();
        if (row.containsKey(upper)) {
            return row.get(upper);
        }
        return row.get(key.toLowerCase());
    }

    static Long longValue(Object value) {
        return ((Number) value).longValue();
    }

    static Integer intValue(Object value) {
        return ((Number) value).intValue();
    }

    static BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    static Boolean booleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    static LocalDateTime dateTimeValue(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(value.toString());
    }

    static LocalDateTime dateTimeValueOrNull(Object value) {
        return value == null ? null : dateTimeValue(value);
    }

    static List<String> normalizedLabels(List<String> labels) {
        if (labels == null) {
            return List.of();
        }
        return labels.stream()
                .map(String::trim)
                .filter(label -> !label.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();
    }

    static List<String> splitLabels(String labels) {
        if (labels == null || labels.isBlank()) {
            return List.of();
        }
        return normalizedLabels(List.of(labels.split(",")));
    }
}
