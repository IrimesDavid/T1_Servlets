package com.csvplatform.analysis.services.helpers;

import com.csvplatform.common.model.ColumnInfo;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CellTypes {
    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+\\.\\d*|\\.\\d+)");
    private static final Set<String> BOOLEANS = Set.of("true", "false", "yes", "no");
    private static final List<DateTimeFormatter> DATES = Stream.of(
                    "uuuu-MM-dd",
                    "dd/MM/uuuu",
                    "MM/dd/uuuu",
                    "dd.MM.uuuu",
                    "dd/MMM/uuuu",
                    "dd/MMMM/uuuu"
            )
            .map(p -> DateTimeFormatter.ofPattern(p, Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.STRICT))
            .toList();

    private CellTypes() {}

    /** @param cell a trimmed, non-missing value */
    public static boolean matches(String cell, ColumnInfo.Type type) {
        return switch (type) {
            case INTEGER -> INTEGER.matcher(cell).matches();
            case DECIMAL -> INTEGER.matcher(cell).matches() || DECIMAL.matcher(cell).matches();
            case BOOLEAN -> BOOLEANS.contains(cell.toLowerCase(Locale.ROOT));
            case DATE    -> isDate(cell);
            case TEXT    -> true;
        };
    }

    private static boolean isDate(String cell) {
        for (DateTimeFormatter f : DATES) {
            try { f.parse(cell); return true; } catch (DateTimeParseException ignored) {}
        }
        return false;
    }
}