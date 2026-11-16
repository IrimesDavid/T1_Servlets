package com.csvplatform.analysis.services.rules;

import com.csvplatform.analysis.services.helpers.CellTypes;
import com.csvplatform.analysis.services.helpers.MissingValues;
import com.csvplatform.common.model.ColumnInfo;
import com.csvplatform.common.model.Issue;

import java.util.ArrayList;
import java.util.List;

public class TypeMismatchRule implements Rule {
    @Override
    public List<Issue> check(Dataset dataset) {
        List<Issue> issues = new ArrayList<>();
        for (ColumnInfo col : dataset.columns()) {
            if (col.inferredType() == ColumnInfo.Type.TEXT) continue;

            List<Integer> rows = new ArrayList<>();
            for (int i = 0; i < dataset.rows().size(); i++) {
                String cell = dataset.rows().get(i).get(col.name());
                if (MissingValues.isMissing(cell)) continue;
                if (!CellTypes.matches(cell.trim(), col.inferredType())) rows.add(i + 1);
            }
            if (!rows.isEmpty()) {
                issues.add(Issue.of(Issue.Category.TYPE_MISMATCH, col.name(), rows,
                        "Expected " + col.inferredType() + ", found incompatible value(s)",
                        Issue.Action.SET_EMPTY));
            }
        }
        return issues;
    }
}
