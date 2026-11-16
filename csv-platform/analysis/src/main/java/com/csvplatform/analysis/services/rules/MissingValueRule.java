package com.csvplatform.analysis.services.rules;

import com.csvplatform.analysis.services.helpers.MissingValues;
import com.csvplatform.common.model.ColumnInfo;
import com.csvplatform.common.model.Issue;

import java.util.ArrayList;
import java.util.List;

public class MissingValueRule implements Rule {
    @Override
    public List<Issue> check(Dataset dataset) {
        List<Issue> issues = new ArrayList<>();
        int total = dataset.rows().size();
        for (ColumnInfo col : dataset.columns()) {
            List<Integer> rows = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                if (MissingValues.isMissing(dataset.rows().get(i).get(col.name()))) rows.add(i + 1);
            }
            if (rows.isEmpty()) continue;
            issues.add(Issue.of(Issue.Category.MISSING_VALUE, col.name(), rows,
                    rows.size() + " missing value(s)", Issue.Action.SET_EMPTY));
        }

        return issues;
    }
}
