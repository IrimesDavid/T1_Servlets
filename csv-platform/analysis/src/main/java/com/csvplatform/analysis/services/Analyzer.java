package com.csvplatform.analysis.services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.csvplatform.analysis.services.helpers.ColumnProfiler;
import com.csvplatform.analysis.services.rules.*;
import com.csvplatform.common.jobs.JobStatus;
import com.csvplatform.common.model.AnalysisReport;
import com.csvplatform.common.jobs.JobStore;
import com.csvplatform.common.model.ColumnInfo;
import com.csvplatform.common.model.FileInfo;
import com.csvplatform.common.model.Issue;

public class Analyzer {
    private final JobStore jobStore;
    private final List<Rule> rules;

    public Analyzer(JobStore jobStore) {
        this.jobStore = jobStore;
        this.rules = List.of(
                new WhiteSpaceRule(),
                new MissingValueRule(),
                new TypeMismatchRule(),
                new DuplicateRowRule(),
                new OutlierRule()
        );
    }

    public AnalysisReport analyze(String jobId) throws IOException {
        AnalysisReport report;

        try {
            Path original = jobStore.originalCsv(jobId);

            jobStore.writeStatus(jobId, JobStatus.of(JobStatus.State.ANALYZING));

            // Read file
            List<Map<String, String>> rows = jobStore.readCsv(original)
                    .orElseThrow(() -> new IllegalArgumentException("Original CSV not found"));


            // Prepare for analysis
            List<String> headers = List.copyOf(rows.get(0).keySet());
            FileInfo fileInfo = new FileInfo(rows.size(), rows.get(0).size(), headers);
            List<ColumnInfo> columns = ColumnProfiler.profile(headers, rows);
            List<Issue> issues = new ArrayList<>();
            Dataset dataset = new Dataset(rows, fileInfo, columns);

            // Run the rules and find the issues
            for (Rule rule : rules) {
                issues.addAll(rule.check(dataset));
            }

            // Generate and store the report
            report = AnalysisReport.create(jobId, fileInfo, columns, issues);
            jobStore.writeReport(jobId, report);

            jobStore.writeStatus(jobId, JobStatus.of(JobStatus.State.ANALYZED));
        } catch(Exception e){
            try {
                jobStore.writeStatus(jobId, JobStatus.failed(e.getMessage()));
            } catch (IOException ignored) {
                // the original error is more important than a failed status write
            }
            throw e;
        }

        return report;
    }
}

