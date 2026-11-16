package com.csvplatform.analysis.services;

import com.csvplatform.common.jobs.JobStatus;
import com.csvplatform.common.jobs.JobStore;
import com.csvplatform.common.model.AnalysisReport;
import com.csvplatform.common.model.Issue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzerTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path TEST_DATA = Path.of(System.getProperty("testdata.dir", "../test-data"));

    @TempDir
    Path tempDir;

    private RecordingJobStore jobStore;
    private String jobId;

    @BeforeEach
    void setUp() throws IOException {
        jobStore = new RecordingJobStore(tempDir.resolve("jobs"));
        jobId = UUID.randomUUID().toString();
        jobStore.create(jobId);
    }

    @Test
    void analyzesMixedDatasetWritesReportAndRecordsStatuses() throws Exception {
        Files.copy(TEST_DATA.resolve("mixed.csv"), jobStore.originalCsv(jobId));

        AnalysisReport report = new Analyzer(jobStore).analyze(jobId);

        assertReportMatches("mixed.expected-report.json", report);
        assertTrue(Files.exists(jobStore.report(jobId)));
        assertReportMatches("mixed.expected-report.json", jobStore.readReport(jobId).orElseThrow());
        assertEquals(List.of(JobStatus.State.ANALYZING, JobStatus.State.ANALYZED), jobStore.states);
    }

    @Test
    void analyzesCleanAndBomSemicolonDatasets() throws Exception {
        for (String name : List.of("clean", "semicolon")) {
            String currentJobId = UUID.randomUUID().toString();
            jobStore.create(currentJobId);
            Files.copy(TEST_DATA.resolve(name + ".csv"), jobStore.originalCsv(currentJobId));

            AnalysisReport report = new Analyzer(jobStore).analyze(currentJobId);

            assertReportMatches(name + ".expected-report.json", report);
            assertTrue(report.issues().isEmpty());
        }
    }

    @Test
    void missingOriginalMarksJobFailedWithoutWritingReport() throws Exception {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> new Analyzer(jobStore).analyze(jobId));

        assertEquals("Original CSV not found", error.getMessage());
        assertEquals(JobStatus.State.FAILED, jobStore.states.get(jobStore.states.size() - 1));
        assertNotNull(jobStore.readStatus(jobId).orElseThrow().message());
        assertFalse(Files.exists(jobStore.report(jobId)));
    }

    @Test
    void rejectsNonUuidJobId() {
        assertThrows(IllegalArgumentException.class, () -> new Analyzer(jobStore).analyze("not-a-uuid"));
    }

    private static void assertReportMatches(String expectedName, AnalysisReport actual) throws IOException {
        AnalysisReport expected = JSON.readValue(TEST_DATA.resolve(expectedName).toFile(), AnalysisReport.class);
        assertEquals(expected.file(), actual.file());
        assertEquals(expected.columns(), actual.columns());
        assertEquals(expected.summary(), actual.summary());
        assertEquals(issueTuples(expected.issues()), issueTuples(actual.issues()));
    }

    private static Set<IssueTuple> issueTuples(List<Issue> issues) {
        return issues.stream()
                .map(issue -> new IssueTuple(issue.category(), issue.column(), issue.rows(), issue.suggestedAction()))
                .collect(Collectors.toSet());
    }

    private record IssueTuple(Issue.Category category, String column, List<Integer> rows, Issue.Action action) {}

    private static final class RecordingJobStore extends JobStore {
        private final List<JobStatus.State> states = new ArrayList<>();

        private RecordingJobStore(Path jobsDir) {
            super(jobsDir);
        }

        @Override
        public void writeStatus(String id, JobStatus status) throws IOException {
            states.add(status.state());
            super.writeStatus(id, status);
        }
    }
}
