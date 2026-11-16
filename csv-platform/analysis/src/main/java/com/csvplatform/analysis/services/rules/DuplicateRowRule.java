package com.csvplatform.analysis.services.rules;

import com.csvplatform.analysis.services.helpers.MissingValues;
import com.csvplatform.common.model.ColumnInfo;
import com.csvplatform.common.model.Issue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DuplicateRowRule implements Rule {
    @Override
    public List<Issue> check(Dataset dataset) {
        Map<List<String>, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < dataset.rows().size(); i++) {
            Map<String, String> row = dataset.rows().get(i);
            List<String> key = new ArrayList<>();
            for (ColumnInfo col : dataset.columns()) {
                String cell = row.get(col.name());
                key.add(MissingValues.isMissing(cell) ? "" : cell.trim());
            }
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(i + 1);
        }

        List<Issue> issues = new ArrayList<>();
        for (List<Integer> rows : groups.values()) {
            if (rows.size() > 1) {
                issues.add(Issue.of(Issue.Category.DUPLICATE_ROW, null, rows,
                        "Identical rows starting with row " + rows.get(0),
                        Issue.Action.REMOVE_ROW));
            }
        }
        return issues;
    }
}
