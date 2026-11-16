package com.csvplatform.common.model;

import java.util.List;

/**
 * One finding of the analysis: a group of rows that share the same problem in the same column.
 * Conventions used by the Analysis service:
 * <ul>
 *   <li>MISSING_VALUE, WHITESPACE, TYPE_MISMATCH, OUTLIER: one issue per column and category, listing all affected rows.</li>
 *   <li>DUPLICATE_ROW: one issue per group of identical rows; column is null and rows lists every row of the group
 *       (the first one is kept by Cleaning).</li>
 *   <li>value is only set when all listed rows share the same value, otherwise null.</li>
 * </ul>
 * Row numbers are 1-based data-row numbers (row 1 is the first row after the header).
 *
 * @param id              assigned by AnalysisReport.create, starting at 1
 * @param column          column name, or null if the issue is not tied to one column
 * @param suggestedAction what Cleaning should do by default
 */
public record Issue(
        int id,
        Category category,
        String column,
        List<Integer> rows,
        String message,
        Action suggestedAction) {

    public enum Category { MISSING_VALUE, TYPE_MISMATCH, DUPLICATE_ROW, OUTLIER, WHITESPACE,
    XMISSING_VALUE, XTYPE_MISMATCH, XDUPLICATE_ROW, XOUTLIER, XWHITESPACE,}

    public enum Action { SET_EMPTY, TRIM, REMOVE_ROW, KEEP_AND_FLAG,
    XSET_EMPTY, XTRIM, XREMOVE_ROW, XKEEP_AND_FLAG,}

    public Issue {
        rows = (rows == null) ? List.of() : List.copyOf(rows);
    }

    /** Issue without an id yet; AnalysisReport.create numbers it. */
    public static Issue of(Category category, String column, List<Integer> rows, String message, Action suggestedAction) {
        return new Issue(0, category, column, rows, message, suggestedAction);
    }

    public Issue withId(int newId) {
        return new Issue(newId, category, column, rows, message, suggestedAction);
    }
}