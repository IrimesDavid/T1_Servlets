package com.csvplatform.analysis.services.helpers;

import com.csvplatform.common.model.ColumnInfo;

import java.util.*;

public final class ColumnProfiler {
    private static final double MAJORITY = 0.8;
    // narrowest first: an INTEGER column is tested before DECIMAL
    private static final List<ColumnInfo.Type> CANDIDATES = List.of(
            ColumnInfo.Type.BOOLEAN, ColumnInfo.Type.INTEGER,
            ColumnInfo.Type.DECIMAL, ColumnInfo.Type.DATE);

    private ColumnProfiler() {}

    public static List<ColumnInfo> profile(List<String> headers, List<Map<String, String>> rows) {
        List<ColumnInfo> result = new ArrayList<>();

        // Keep only distinct rows for type inference.
        Set<Map<String, String>> uniqueRows = new LinkedHashSet<>();
        for (Map<String, String> row : rows) {
            uniqueRows.add(new LinkedHashMap<>(row));
        }

        for (String header : headers) {
            int missing = 0, present = 0;
            Map<ColumnInfo.Type, Integer> fits = new EnumMap<>(ColumnInfo.Type.class);

            for (Map<String, String> row : rows) {
                if (MissingValues.isMissing(row.get(header))) missing++;
            }

            // Duplicate rows do not influence type inference, but each row contributes to missingCount.
            for (Map<String, String> row : uniqueRows) {
                String cell = row.get(header);
                if (MissingValues.isMissing(cell)) continue;
                present++;
                String v = cell.trim();
                for (ColumnInfo.Type t : CANDIDATES) {
                    if (CellTypes.matches(v, t)) fits.merge(t, 1, Integer::sum);
                }
            }
            result.add(new ColumnInfo(header, infer(fits, present), missing));
        }
        return result;
    }

    private static ColumnInfo.Type infer(Map<ColumnInfo.Type, Integer> fits, int present) {
        if (present == 0) return ColumnInfo.Type.TEXT;

        int integerFits = fits.getOrDefault(ColumnInfo.Type.INTEGER, 0);
        int decimalFits = fits.getOrDefault(ColumnInfo.Type.DECIMAL, 0);
        // DECIMAL also matches integers, so this detects actual decimal values
        boolean hasDecimalValues = decimalFits > integerFits;

        if (decimalFits >= present * MAJORITY) {
            if (decimalFits > integerFits) {
                return ColumnInfo.Type.DECIMAL;
            }
            return ColumnInfo.Type.INTEGER;
        }

        for (ColumnInfo.Type t : CANDIDATES) {
            if (fits.getOrDefault(t, 0) >= present * MAJORITY) return t;
        }
        return ColumnInfo.Type.TEXT;
    }
}