package com.csvplatform.common.model;

import java.util.List;

/**
 * Basic facts about the analyzed file.
 *
 * @param rows    number of data rows (header excluded)
 * @param columns number of columns
 * @param headers column names in file order
 */
public record FileInfo(int rows, int columns, List<String> headers) {

    public FileInfo {
        headers = (headers == null) ? List.of() : List.copyOf(headers);
    }
}