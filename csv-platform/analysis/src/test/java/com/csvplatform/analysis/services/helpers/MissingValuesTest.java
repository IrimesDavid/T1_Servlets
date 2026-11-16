package com.csvplatform.analysis.services.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingValuesTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "NA", "N/A", "null", "NULL", "-", "?", "none", "NaN",
            "  NONE  ", " n/a "})
    void identifiesMissingValues(String value) {
        assertTrue(MissingValues.isMissing(value));
    }

    @Test
    void doesNotTreatOrdinaryValuesAsMissing() {
        assertFalse(MissingValues.isMissing("0"));
        assertFalse(MissingValues.isMissing("abc"));
        assertFalse(MissingValues.isMissing("N A"));
    }
}
