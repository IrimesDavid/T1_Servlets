package com.csvplatform.analysis.services.rules;

import com.csvplatform.common.model.ColumnInfo;
import com.csvplatform.common.model.FileInfo;

import java.util.List;
import java.util.Map;

public record Dataset(List<Map<String, String>> rows, FileInfo fileInfo, List<ColumnInfo> columns) {
    public Dataset {
        rows = (rows == null) ? List.of() : List.copyOf(rows);
        columns = (columns == null) ? List.of() : List.copyOf(columns);
    }
}
