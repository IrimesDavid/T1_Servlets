package com.csvplatform.cleaning.services;

import com.csvplatform.common.jobs.JobStatus;
import com.csvplatform.common.jobs.JobStore;
import com.csvplatform.common.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;

public class Cleaner {
    private final JobStore jobStore;

    public Cleaner(JobStore jobStore){
        this.jobStore = jobStore;
    }

    public void clean(String jobId) throws IOException {
        Path reportPath = jobStore.report(jobId);
        Path original = jobStore.originalCsv(jobId);
        Path cleaned = jobStore.cleanedCsv(jobId);

        if (!Files.exists(reportPath)) {
            throw new IllegalArgumentException("No analysis report for job " + jobId);
        }

        if(!Files.exists(original)) {
            throw new IllegalArgumentException("No .csv file found for job " + jobId);
        }

        try{
            jobStore.writeStatus(jobId, JobStatus.of(JobStatus.State.CLEANING));

            AnalysisReport report = jobStore.readReport(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Report not found"));
            List<Map<String, String>> rows = jobStore.readCsv(original)
                    .orElseThrow(() -> new IllegalArgumentException("Original CSV not found"));

            if (rows.size() != report.file().rows()) {
                throw new IllegalArgumentException("Report does not match the original file");
            }

            // Fix issues based on the suggested actions
            resolveIssues(rows, report);

            // Write the cleaned file
            jobStore.writeCsv(cleaned, rows);

            jobStore.writeStatus(jobId, JobStatus.of(JobStatus.State.DONE));
        } catch(Exception e){
            try{
                jobStore.writeStatus(jobId, JobStatus.failed(e.getMessage()));
            } catch (IOException ignored){
                // let the original error pass through
            }
            throw e;
        }
    }

    /** Applies the actions suggested in the report, based on category */
    private void resolveIssues(List<Map<String, String>> rows, AnalysisReport report) {
        Set<Integer> rowsToRemove = new HashSet<>();

        for (Issue issue : report.issues()) {
            if (issue.suggestedAction() == Issue.Action.REMOVE_ROW){
                rowsToRemove.addAll(issue.rows().subList(1, issue.rows().size()));
                continue;
            }

            if (issue.column() == null || !report.file().headers().contains(issue.column())) {
                throw new IllegalArgumentException("Report refers to unknown column: " + issue.column());
            }

            UnaryOperator<String> transform;
            switch (issue.suggestedAction()) {
                case SET_EMPTY:
                    transform = cell -> ""; // convention: the normalized empty field is '' in the resulted CSV
                    break;
                case TRIM:
                    transform = String::strip;
                    break;
                case KEEP_AND_FLAG: // e.g. outliers are kept and only flagged inside the report
                    continue;
                default:
                    throw new IllegalArgumentException("Unsupported action: " + issue.suggestedAction());
            }

            for (int rowNumber : issue.rows()) {  // 1-indexed so need to subtract 1
                if (rowNumber < 1 || rowNumber > rows.size()) {
                    throw new IllegalArgumentException("Invalid row number: " + rowNumber);
                }

                Map<String, String> row = rows.get(rowNumber - 1);
                String cell = row.get(issue.column());
                row.put(issue.column(), transform.apply(cell == null ? "" : cell));
            }
        }

        // Remove rows after all other transformations.
        for (int rowNumber : rowsToRemove) {
            if (rowNumber < 1 || rowNumber > rows.size()) {
                throw new IllegalArgumentException("Invalid row number: " + rowNumber);
            }
        }

        rowsToRemove.stream()
                .sorted(Comparator.reverseOrder())
                .forEach(rowNumber -> rows.remove(rowNumber - 1));
    }

}














