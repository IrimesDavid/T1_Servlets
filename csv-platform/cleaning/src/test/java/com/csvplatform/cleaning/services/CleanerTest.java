package com.csvplatform.cleaning.services;

import com.csvplatform.common.jobs.JobStatus;
import com.csvplatform.common.jobs.JobStore;
import com.csvplatform.common.model.AnalysisReport;
import com.csvplatform.common.model.FileInfo;
import com.csvplatform.common.model.Issue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CleanerTest {
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
    void trimsCells() throws Exception {
        prepare(List.of(row("name", "  Alice  ")), report(1, List.of("name"),
                issue(Issue.Category.WHITESPACE, "name", List.of(1), Issue.Action.TRIM)));

        new Cleaner(jobStore).clean(jobId);

        assertEquals(List.of(row("name", "Alice")), readCleaned());
        assertSuccessfulStatuses();
    }

    @Test
    void setsCellsEmpty() throws Exception {
        prepare(List.of(row("name", "Alice", "city", "Cluj"), row("name", "Bob", "city", "Iasi")),
                report(2, List.of("name", "city"),
                        issue(Issue.Category.MISSING_VALUE, "city", List.of(1), Issue.Action.SET_EMPTY)));

        new Cleaner(jobStore).clean(jobId);

        assertEquals(List.of(row("name", "Alice", "city", ""), row("name", "Bob", "city", "Iasi")),
                readCleaned());
        assertEquals(List.of("name", "city"), new ArrayList<>(readCleaned().get(0).keySet()));
        assertSuccessfulStatuses();
    }

    @Test
    void keepsFirstRowOfDuplicateGroupAndRemovesLaterRows() throws Exception {
        prepare(List.of(row("name", "one"), row("name", "two"), row("name", "three"),
                        row("name", "four"), row("name", "five")),
                report(5, List.of("name"),
                        issue(Issue.Category.DUPLICATE_ROW, null, List.of(2, 4, 5), Issue.Action.REMOVE_ROW)));

        new Cleaner(jobStore).clean(jobId);

        assertEquals(List.of(row("name", "one"), row("name", "two"), row("name", "three")), readCleaned());
        assertSuccessfulStatuses();
    }

    @Test
    void leavesKeepAndFlagCellsUnchanged() throws Exception {
        List<Map<String, String>> original = List.of(row("value", "300"), row("value", "10"));
        prepare(original, report(2, List.of("value"),
                issue(Issue.Category.OUTLIER, "value", List.of(1), Issue.Action.KEEP_AND_FLAG)));

        new Cleaner(jobStore).clean(jobId);

        assertEquals(original, readCleaned());
        assertSuccessfulStatuses();
    }

    @Test
    void appliesCellChangesBeforeRemovingSeveralDuplicateGroups() throws Exception {
        List<Map<String, String>> original = List.of(
                row("name", "  A  ", "age", "1"),
                row("name", "B", "age", "2"),
                row("name", "B", "age", "2"),
                row("name", "D", "age", "4"),
                row("name", "E", "age", "5"),
                row("name", "E", "age", "5"));
        prepare(original, report(6, List.of("name", "age"),
                issue(Issue.Category.WHITESPACE, "name", List.of(1), Issue.Action.TRIM),
                issue(Issue.Category.MISSING_VALUE, "age", List.of(1), Issue.Action.SET_EMPTY),
                issue(Issue.Category.DUPLICATE_ROW, null, List.of(2, 3), Issue.Action.REMOVE_ROW),
                issue(Issue.Category.DUPLICATE_ROW, null, List.of(5, 6), Issue.Action.REMOVE_ROW)));

        new Cleaner(jobStore).clean(jobId);

        assertEquals(List.of(
                row("name", "A", "age", ""),
                row("name", "B", "age", "2"),
                row("name", "D", "age", "4"),
                row("name", "E", "age", "5")), readCleaned());
        assertSuccessfulStatuses();
    }

    @Test
    void rejectsReportWithWrongRowCountAndMarksJobFailed() throws Exception {
        prepare(List.of(row("name", "Alice")), report(2, List.of("name")));

        assertThrows(IllegalArgumentException.class, () -> new Cleaner(jobStore).clean(jobId));

        assertEquals(JobStatus.State.FAILED, lastStatus());
    }

    @Test
    void rejectsMissingReportAndMissingOriginal() throws Exception {
        Files.writeString(jobStore.originalCsv(jobId), "name\nAlice\n");
        assertThrows(IllegalArgumentException.class, () -> new Cleaner(jobStore).clean(jobId));

        jobStore.delete(jobId);
        jobStore.create(jobId);
        jobStore.writeReport(jobId, report(1, List.of("name")));
        assertThrows(IllegalArgumentException.class, () -> new Cleaner(jobStore).clean(jobId));
    }

    @Test
    void cleansTheMixedFixture() throws Exception {
        Files.copy(TEST_DATA.resolve("mixed.csv"), jobStore.originalCsv(jobId));
        ObjectNode expectedTree = (ObjectNode) JSON.readTree(
                TEST_DATA.resolve("mixed.expected-report.json").toFile());
        expectedTree.put("jobId", jobId);
        jobStore.writeReport(jobId, JSON.treeToValue(expectedTree, AnalysisReport.class));

        new Cleaner(jobStore).clean(jobId);

        assertEquals(readCsv(TEST_DATA.resolve("mixed.expected-cleaned.csv")), readCleaned());
        assertSuccessfulStatuses();
    }

    private void prepare(List<Map<String, String>> rows, AnalysisReport report) throws IOException {
        jobStore.writeCsv(jobStore.originalCsv(jobId), rows);
        jobStore.writeReport(jobId, report);
    }

    private List<Map<String, String>> readCleaned() throws IOException {
        return jobStore.readCsv(jobStore.cleanedCsv(jobId)).orElseThrow();
    }

    private void assertSuccessfulStatuses() {
        assertEquals(List.of(JobStatus.State.CLEANING, JobStatus.State.DONE), jobStore.states);
    }

    private JobStatus.State lastStatus() throws IOException {
        return jobStore.readStatus(jobId).orElseThrow().state();
    }

    private static AnalysisReport report(int rows, List<String> headers, Issue... issues) {
        return AnalysisReport.create(UUID.randomUUID().toString(),
                new FileInfo(rows, headers.size(), headers), List.of(), List.of(issues));
    }

    private static Issue issue(Issue.Category category, String column, List<Integer> rows, Issue.Action action) {
        return Issue.of(category, column, rows, "test issue", action);
    }

    private static Map<String, String> row(String key, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put(key, value);
        return row;
    }

    private static Map<String, String> row(String key1, String value1, String key2, String value2) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put(key1, value1);
        row.put(key2, value2);
        return row;
    }

    private static List<Map<String, String>> readCsv(Path path) throws IOException {
        return com.csvplatform.common.utils.CsvConverter.readCsv(path);
    }

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
