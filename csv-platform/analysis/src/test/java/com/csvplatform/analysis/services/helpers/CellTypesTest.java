package com.csvplatform.analysis.services.helpers;

import com.csvplatform.common.model.ColumnInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellTypesTest {

    @Test
    void matchesIntegerValues() {
        assertMatches(ColumnInfo.Type.INTEGER, true, "42", "-7", "+3");
        assertMatches(ColumnInfo.Type.INTEGER, false, "3.5", "abc");
    }

    @Test
    void matchesDecimalValues() {
        assertMatches(ColumnInfo.Type.DECIMAL, true, "3.5", ".5", "42");
        assertMatches(ColumnInfo.Type.DECIMAL, false, "1,5", "abc");
    }

    @Test
    void matchesBooleanValues() {
        assertMatches(ColumnInfo.Type.BOOLEAN, true, "true", "FALSE", "yes", "No");
        assertMatches(ColumnInfo.Type.BOOLEAN, false, "1", "0", "maybe");
    }

    @Test
    void matchesStrictDates() {
        assertMatches(ColumnInfo.Type.DATE, true, "2024-01-31");
        assertMatches(ColumnInfo.Type.DATE, false, "2024-02-30", "abc");
    }

    @Test
    void textMatchesAnyValue() {
        assertTrue(CellTypes.matches("", ColumnInfo.Type.TEXT));
        assertTrue(CellTypes.matches("anything", ColumnInfo.Type.TEXT));
    }

    private static void assertMatches(ColumnInfo.Type type, boolean expected, String... values) {
        for (String value : values) {
            if (expected) {
                assertTrue(CellTypes.matches(value, type), value);
            } else {
                assertFalse(CellTypes.matches(value, type), value);
            }
        }
    }
}
