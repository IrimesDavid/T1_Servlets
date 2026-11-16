package com.csvplatform.analysis.services.helpers;

import com.csvplatform.common.model.ColumnInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnProfilerTest {

    @Test
    void infersIntegerFromTenIntegersAndOneTextValue() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (int value = 1; value <= 10; value++) {
            rows.add(row("value", Integer.toString(value)));
        }
        rows.add(row("value", "text"));

        assertEquals(ColumnInfo.Type.INTEGER, profile(rows).inferredType());
    }

    @Test
    void infersDecimalWhenIntegerAndDecimalValuesAreMixed() {
        assertEquals(ColumnInfo.Type.DECIMAL,
                profile(rows("value", "1", "2.5", "3.5")).inferredType());
    }

    @Test
    void infersTextWhenOnlyHalfTheValuesAreNumbers() {
        assertEquals(ColumnInfo.Type.TEXT,
                profile(rows("value", "1", "2", "a", "b")).inferredType());
    }

    @Test
    void infersBooleanValues() {
        assertEquals(ColumnInfo.Type.BOOLEAN,
                profile(rows("value", "true", "false", "TRUE")).inferredType());
    }

    @Test
    void infersIsoDates() {
        assertEquals(ColumnInfo.Type.DATE,
                profile(rows("value", "2024-01-31", "2023-12-01")).inferredType());
    }

    @Test
    void allMissingValuesRemainTextAndCountEveryRow() {
        ColumnInfo profile = profile(rows("value", "", "", "NA", "NA"));

        assertEquals(ColumnInfo.Type.TEXT, profile.inferredType());
        assertEquals(4, profile.missingCount());
    }

    @Test
    void missingCountIncludesTokensEmptyCellsAndRowsWithoutTheColumn() {
        List<Map<String, String>> rows = List.of(
                row("value", ""),
                row("value", "NA"),
                new LinkedHashMap<>(),
                row("value", "present"));

        assertEquals(3, profile(rows).missingCount());
    }

    private static ColumnInfo profile(List<Map<String, String>> rows) {
        return ColumnProfiler.profile(List.of("value"), rows).get(0);
    }

    private static List<Map<String, String>> rows(String header, String... values) {
        return java.util.Arrays.stream(values).map(value -> row(header, value)).toList();
    }

    private static Map<String, String> row(String key, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put(key, value);
        return row;
    }
}
