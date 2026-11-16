package com.csvplatform.gateway.services;

import com.csvplatform.common.jobs.JobStore;
import com.csvplatform.common.model.AnalysisReport;
import com.csvplatform.common.model.FileInfo;
import com.csvplatform.gateway.services.helpers.ServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GatewayServiceTest {
    private static final Path TEST_DATA = Path.of(System.getProperty("testdata.dir", "../test-data"));

    @TempDir
    Path tempDir;

    private JobStore jobStore;
    private StubServiceClient client;
    private GatewayService gateway;

    @BeforeEach
    void setUp() {
        jobStore = new JobStore(tempDir.resolve("jobs"));
        client = new StubServiceClient(jobStore);
        gateway = new GatewayService(jobStore, client);
    }

    @Test
    void uploadsValidFileAndCallsServicesInOrderWithTheSameJobId() throws Exception {
        AnalysisReport report;
        try (InputStream input = Files.newInputStream(TEST_DATA.resolve("clean.csv"))) {
            report = gateway.upload("data.csv", input);
        }

        assertTrue(UUID.fromString(report.jobId()).toString().equals(report.jobId()));
        assertTrue(Files.exists(jobStore.originalCsv(report.jobId())));
        assertTrue(Files.exists(jobStore.cleanedCsv(report.jobId())));
        assertEquals(List.of("analyze:" + report.jobId(), "clean:" + report.jobId()), client.calls);
        assertEquals(List.of(report.jobId()), listJobs());
    }

    @Test
    void analysisFailureDeletesJobAndDoesNotCallCleaning() {
        client.analysisFailure = new IOException("unavailable");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> gateway.upload("data.csv", input("id\n1\n")));

        assertTrue(error.getMessage().contains("Analysis"));
        assertEquals(1, client.calls.size());
        assertTrue(listJobs().isEmpty());
    }

    @Test
    void cleaningFailureDeletesJob() {
        client.cleaningFailure = new IOException("unavailable");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> gateway.upload("data.csv", input("id\n1\n")));

        assertTrue(error.getMessage().contains("Cleaning"));
        assertEquals(2, client.calls.size());
        assertTrue(listJobs().isEmpty());
    }

    @Test
    void rejectsWrongExtensionWithoutCreatingAJob() {
        assertThrows(IllegalArgumentException.class,
                () -> gateway.upload("data.txt", input("id\n1\n")));
        assertTrue(listJobs().isEmpty());
    }

    @Test
    void rejectsInvalidCsvAndDeletesItsJob() {
        assertThrows(IllegalArgumentException.class, () -> gateway.upload("data.csv", input("")));
        assertTrue(listJobs().isEmpty());
    }

    private List<String> listJobs() {
        Path jobsDir = tempDir.resolve("jobs");
        if (!Files.exists(jobsDir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(jobsDir)) {
            return paths.map(path -> path.getFileName().toString()).toList();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static InputStream input(String value) {
        return new java.io.ByteArrayInputStream(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static final class StubServiceClient extends ServiceClient {
        private final JobStore jobStore;
        private final List<String> calls = new ArrayList<>();
        private IOException analysisFailure;
        private IOException cleaningFailure;

        private StubServiceClient(JobStore jobStore) {
            super("http://localhost", "http://localhost");
            this.jobStore = jobStore;
        }

        @Override
        public void analyze(String jobId) throws IOException {
            calls.add("analyze:" + jobId);
            if (analysisFailure != null) throw analysisFailure;

            List<Map<String, String>> rows = jobStore.readCsv(jobStore.originalCsv(jobId)).orElseThrow();
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            jobStore.writeReport(jobId, AnalysisReport.create(jobId,
                    new FileInfo(rows.size(), headers.size(), headers), List.of(), List.of()));
        }

        @Override
        public void clean(String jobId) throws IOException {
            calls.add("clean:" + jobId);
            if (cleaningFailure != null) throw cleaningFailure;

            jobStore.writeCsv(jobStore.cleanedCsv(jobId),
                    jobStore.readCsv(jobStore.originalCsv(jobId)).orElseThrow());
        }
    }
}
