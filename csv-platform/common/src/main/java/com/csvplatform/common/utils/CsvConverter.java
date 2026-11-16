package com.csvplatform.common.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Utility class
public final class CsvConverter {
    private static final int SNIFF_LIMIT = 1 << 20; // 1 MB

    private CsvConverter() {
    }

    /**
     * Reads a CSV file into a list of maps.
     * Each map represents one row, using column headers as keys.
     */
    public static List<Map<String, String>> readCsv(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {

            // Skip UTF-8 BOM if present.
            reader.mark(1);
            if (reader.read() != '\uFEFF') {
                reader.reset();
            }

            // Detect comma or semicolon delimiter.
            char delimiter = detectDelimiter(reader);

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();

            try (CSVParser parser = format.parse(reader)) {

                List<String> headers = parser.getHeaderNames().stream().map(String::strip).toList(); // remove whitespaces from headers now as we won't touch them later

                if (headers.isEmpty() || headers.stream().allMatch(String::isBlank)) {
                    throw new IllegalArgumentException("CSV file has no valid header");
                }

                if (headers.stream().distinct().count() != headers.size()) {
                    throw new IllegalArgumentException("CSV contains duplicate column names");
                }

                List<Map<String, String>> rows = new ArrayList<>();

                for (CSVRecord record : parser) {
                    Map<String, String> row = new LinkedHashMap<>();

                    for (int i = 0; i < headers.size(); i++) {
                        String value = i < record.size() ? record.get(i) : "";
                        row.put(headers.get(i), value);
                    }

                    rows.add(row);
                }

                if (rows.isEmpty()) {
                    throw new IllegalArgumentException("CSV file contains no data rows");
                }

                return rows;
            }
        }
    }

    /**
     * Writes a list of maps into a CSV file.
     * The map keys become the CSV headers.
     */
    public static void writeCsv(
            Path file,
            List<Map<String, String>> rows
    ) throws IOException {

        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Cannot write CSV without headers");
        }

        List<String> headers = new ArrayList<>(rows.get(0).keySet());

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(new String[0]))
                .build();

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {

            for (Map<String, String> row : rows) {
                List<String> values = new ArrayList<>();

                for (String header : headers) {
                    values.add(row.getOrDefault(header, ""));
                }

                printer.printRecord(values);
            }
        }
    }

    private static char detectDelimiter(BufferedReader reader) throws IOException {
        reader.mark(SNIFF_LIMIT);

        String firstLine = reader.readLine();

        reader.reset();

        if (firstLine == null) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        long commas = firstLine.chars().filter(c -> c == ',').count();
        long semicolons = firstLine.chars().filter(c -> c == ';').count();

        return semicolons > commas ? ';' : ',';
    }
}