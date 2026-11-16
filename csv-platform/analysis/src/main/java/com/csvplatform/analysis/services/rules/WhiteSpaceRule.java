package com.csvplatform.analysis.services.rules;

import com.csvplatform.common.model.ColumnInfo;
import com.csvplatform.common.model.Issue;

import java.util.ArrayList;
import java.util.List;

public class WhiteSpaceRule implements Rule {
    @Override
    public List<Issue> check(Dataset dataset) {
        List<Issue> issues = new ArrayList<>();
        for (ColumnInfo col : dataset.columns()) {
            String name = col.name();

            List<Integer> rows = new ArrayList<>();
            for (int i = 0; i < dataset.rows().size(); i++) {
                String cell = dataset.rows().get(i).get(name);
                if (cell != null && !cell.equals(cell.strip())) rows.add(i + 1); // rows are indexed from 1
            }
            if (!rows.isEmpty()) {
                issues.add(Issue.of(Issue.Category.WHITESPACE, name, rows,
                        "Cells with leading or trailing whitespace", Issue.Action.TRIM));
            }
        }
        return issues;
    }
}
