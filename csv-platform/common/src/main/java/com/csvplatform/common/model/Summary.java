package com.csvplatform.common.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts of issues (not of cells or rows): totalIssues is the number of Issue entries,
 * byCategory only contains categories that occur.
 */
public record Summary(int totalIssues, Map<Issue.Category, Integer> byCategory) {

    public Summary {
        byCategory = (byCategory == null)
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(byCategory));
    }

    public static Summary of(List<Issue> issues) {
        Map<Issue.Category, Integer> categoryCounts = new EnumMap<>(Issue.Category.class);
        for (Issue issue : issues) {
            categoryCounts.merge(issue.category(), 1, Integer::sum);
        }
        return new Summary(issues.size(), categoryCounts);
    }
}