package com.csvplatform.common.jobs;

import com.csvplatform.common.model.AnalysisReport;
import com.csvplatform.common.utils.CsvConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Knows where each job's files live on the shared volume and what state a job is in.
 * Layout: {basePath}/jobs/{jobId}/original.csv | report.json | cleaned.csv | status.json
 */
public class JobStore {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path jobsDir;

    public JobStore(Path jobsDir) {
        this.jobsDir = jobsDir;
    }

    // ---------------- paths ----------------

    /** @throws IllegalArgumentException if jobId is not a UUID */
    private Path jobDir(String jobId) {
        if (jobId == null || !UUID_PATTERN.matcher(jobId).matches()) {
            throw new IllegalArgumentException("Invalid job id");
        }
        return jobsDir.resolve(jobId);
    }

    public Path originalCsv(String jobId) {
        return jobDir(jobId).resolve("original.csv");
    }

    public Path report(String jobId) {
        return jobDir(jobId).resolve("report.json");
    }

    public Path cleanedCsv(String jobId) {
        return jobDir(jobId).resolve("cleaned.csv");
    }

    public Path status(String jobId) {
        return jobDir(jobId).resolve("status.json");
    }

    // ---------------- job folder lifecycle ----------------

    /** Creates the job folder (and the jobs folder if needed). Does nothing if it already exists. */
    public void create(String jobId) throws IOException {
        Files.createDirectories(jobDir(jobId));
    }

    public boolean exists(String jobId) {
        return Files.isDirectory(jobDir(jobId));
    }

    /** Deletes the job folder with everything in it. Does nothing if the job does not exist. */
    public void delete(String jobId) throws IOException {
        Path dir = jobDir(jobId);
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ---------------- status.json ----------------

    /** @return the job's status, or empty if no status has been written yet */
    public Optional<JobStatus> readStatus(String jobId) throws IOException {
        Path file = status(jobId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(JSON.readValue(file.toFile(), JobStatus.class));
    }

    /** Writes status.json (the job folder must exist). Readers never see a half-written file. */
    public void writeStatus(String jobId, JobStatus status) throws IOException {
        Path target = status(jobId);
        Path tmp = tempFor(target);
        try {
            JSON.writeValue(tmp.toFile(), status);
            moveIntoPlace(tmp, target);
        }finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- report.json ----------------

    public Optional<AnalysisReport> readReport(String jobId) throws IOException {
        Path file = report(jobId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(JSON.readValue(file.toFile(), AnalysisReport.class));
    }

    public void writeReport(String jobId, AnalysisReport report) throws IOException {
        Path target = report(jobId);
        Path tmp = tempFor(target);
        try {
            JSON.writeValue(tmp.toFile(), report);
            moveIntoPlace(tmp, target);
        }finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- original/cleaned.csv ----------------

    public Optional<List<Map<String, String>>> readCsv(Path target) throws IOException {
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        return Optional.of(CsvConverter.readCsv(target));
    }

    public void writeCsv(
            Path target,
            List<Map<String, String>> rows
    ) throws IOException {
        Path tmp = tempFor(target);

        try {
            CsvConverter.writeCsv(tmp, rows);
            moveIntoPlace(tmp, target);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- safe file writing ----------------

    /** A temporary file next to the target; write to it, then call moveIntoPlace. */
    private Path tempFor(Path target) {
        return target.resolveSibling(target.getFileName() + ".tmp");
    }

    /** Replaces target with tmp in one step, so a reader sees either the old or the new file, never half of one. */
    private void moveIntoPlace(Path tmp, Path target) throws IOException {
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}