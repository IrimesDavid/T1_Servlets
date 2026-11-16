package com.csvplatform.common.model;

/**
 * Per-column profile.
 *
 * @param inferredType the type most values of the column have
 * @param missingCount number of missing cells in the column
 */
public record ColumnInfo(String name, Type inferredType, int missingCount) {

    public enum Type { INTEGER, DECIMAL, BOOLEAN, DATE, TEXT }
}