package com.csvplatform.analysis.services.helpers;

import java.util.Locale;
import java.util.Set;

public final class MissingValues {
    private static final Set<String> TOKENS =
            Set.of("na", "n/a", "null", "none", "nan", "-", "?");

    private MissingValues() {}

    public static boolean isMissing(String cell) {
        if (cell == null) return true;
        String v = cell.trim();
        return v.isEmpty() || TOKENS.contains(v.toLowerCase(Locale.ROOT));
    }
}