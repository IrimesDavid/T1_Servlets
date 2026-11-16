package com.csvplatform.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Content of report.json, written by Analysis and read by Cleaning and the Gateway (which also sends it to the browser).
 */
public record AnalysisReport(
        String jobId,
        FileInfo file,
        List<ColumnInfo> columns,
        List<Issue> issues,
        Summary summary) {

    public AnalysisReport {
        columns = (columns == null) ? List.of() : List.copyOf(columns);
        issues = (issues == null) ? List.of() : List.copyOf(issues);
    }

    /**
     * Builds a report from issues that have no id yet: numbers them 1..n in the given order
     * and computes the summary.
     */
    public static AnalysisReport create(String jobId, FileInfo file, List<ColumnInfo> columns, List<Issue> issues) {
        List<Issue> numbered = new ArrayList<>(issues.size());
        int next = 1;
        for (Issue issue : issues) {
            numbered.add(issue.withId(next++));
        }
        return new AnalysisReport(jobId, file, columns, numbered, Summary.of(numbered));
    }
}