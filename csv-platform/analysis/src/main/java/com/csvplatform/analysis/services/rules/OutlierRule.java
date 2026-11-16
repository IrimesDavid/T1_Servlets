package com.csvplatform.analysis.services.rules;

import com.csvplatform.analysis.services.helpers.CellTypes;
import com.csvplatform.analysis.services.helpers.MissingValues;
import com.csvplatform.common.model.ColumnInfo;
import com.csvplatform.common.model.Issue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OutlierRule implements Rule {
    private static final int MIN_VALUES = 3;
    private static final double K = 1.5;

    @Override
    public List<Issue> check(Dataset dataset) {
        List<Issue> issues = new ArrayList<>();
        for (ColumnInfo col : dataset.columns()) {
            if (col.inferredType() != ColumnInfo.Type.INTEGER
                    && col.inferredType() != ColumnInfo.Type.DECIMAL) continue;

            List<Integer> rowNumbers = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            for (int i = 0; i < dataset.rows().size(); i++) {
                String cell = dataset.rows().get(i).get(col.name());
                if (MissingValues.isMissing(cell)) continue;
                String v = cell.trim();
                if (!CellTypes.matches(v, col.inferredType())) continue;   // mismatches are reported elsewhere
                rowNumbers.add(i + 1);
                values.add(Double.parseDouble(v));
            }
            if (values.size() < MIN_VALUES) continue;

            double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
            double q1 = quantile(sorted, 0.25), q3 = quantile(sorted, 0.75);
            double iqr = q3 - q1;
            if (iqr == 0) continue;

            double low = q1 - K * iqr, high = q3 + K * iqr;
            List<Integer> outlierRows = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) < low || values.get(i) > high) outlierRows.add(rowNumbers.get(i));
            }
            if (!outlierRows.isEmpty()) {
                issues.add(Issue.of(Issue.Category.OUTLIER, col.name(), outlierRows,
                        String.format(Locale.ROOT, "Outside IQR bounds [%.2f, %.2f] (heuristic)", low, high),
                        Issue.Action.KEEP_AND_FLAG));
            }
        }
        return issues;
    }

    // linear interpolation between closest ranks (same as Excel's QUARTILE.INC)
    private static double quantile(double[] sorted, double p) {
        double pos = p * (sorted.length - 1);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        return sorted[lo] + (pos - lo) * (sorted[hi] - sorted[lo]);
    }
}