package com.csvplatform.gateway.services;

import com.csvplatform.common.jobs.JobStatus;
import com.csvplatform.common.jobs.JobStore;
import com.csvplatform.common.model.AnalysisReport;
import com.csvplatform.common.utils.CsvConverter;
import com.csvplatform.gateway.services.helpers.ServiceClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GatewayService {
    private final JobStore jobStore;
    private final ServiceClient serviceClient;

    public GatewayService(JobStore jobStore, ServiceClient serviceClient) {
        this.jobStore = jobStore;
        this.serviceClient = serviceClient;
    }

    /**
     * Creates a job, stores and validates the file, runs Analysis then Cleaning, returns the report.
     * If anything fails after the job was created, the job folder is deleted.
     *
     * @throws IllegalArgumentException if the file is not an acceptable CSV
     * @throws IllegalStateException    if Analysis or Cleaning is unavailable or failed
     */
    public AnalysisReport upload(String filename, InputStream content) throws IOException {
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are accepted");
        }

        // create a new job
        String jobId = UUID.randomUUID().toString();
        jobStore.create(jobId);

        try {
            Path original = jobStore.originalCsv(jobId);
            Files.copy(content, original);
            try {
                jobStore.readCsv(original); // validates header, duplicate columns, data rows...
            } catch (IOException | UncheckedIOException e) {
                throw new IllegalArgumentException("The file could not be read as a UTF-8 CSV");
            }
            jobStore.writeStatus(jobId, JobStatus.of(JobStatus.State.UPLOADED));

            try {
                serviceClient.analyze(jobId);
            } catch (IOException | IllegalStateException e) {
                throw new IllegalStateException("Analysis service is unavailable or failed", e);
            }
            try {
                serviceClient.clean(jobId);
            } catch (IOException | IllegalStateException e) {
                throw new IllegalStateException("Cleaning service is unavailable or failed", e);
            }

            return jobStore.readReport(jobId)
                    .orElseThrow(() -> new IllegalStateException("Report missing after analysis"));
        } catch (Exception e) {
            jobStore.delete(jobId); // delete the job we have created if something went wrong
            throw e;
        }
    }

    /** @throws IllegalArgumentException if the id is invalid or there is no cleaned file for it */
    public Path cleanedCsv(String jobId) {
        Path cleaned = jobStore.cleanedCsv(jobId);   // throws IllegalArgumentException for a bad id
        if (!Files.exists(cleaned)) {
            throw new IllegalArgumentException("Cleaned file not available for this job");
        }
        return cleaned;
    }

    /** Idempotent. */
    public void clear(String jobId) throws IOException {
        jobStore.delete(jobId);
    }

    public Map<String, String> health() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("gateway", "UP");
        result.put("analysis", serviceClient.isAnalysisUp() ? "UP" : "DOWN");
        result.put("cleaning", serviceClient.isCleaningUp() ? "UP" : "DOWN");
        return result;
    }
}