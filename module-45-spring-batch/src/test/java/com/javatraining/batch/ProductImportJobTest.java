package com.javatraining.batch;

import com.javatraining.batch.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @SpringBatchTest — adds to the application context:
 *   JobLauncherTestUtils  — launch full jobs or individual steps programmatically
 *   JobRepositoryTestUtils — clean up BATCH_* metadata tables between tests
 *   StepScopeTestExecutionListener — activates step scope for @StepScope bean injection in tests
 *   JobScopeTestExecutionListener  — activates job scope for @JobScope bean injection in tests
 *
 * Must be combined with @SpringBootTest (or @ContextConfiguration) to provide the
 * actual Spring context. @SpringBatchTest alone does not load the application context.
 *
 * JobLauncherTestUtils.launchJob(params):
 *   Runs the entire job (all steps) synchronously. Returns the JobExecution after completion.
 *   The @Autowired Job bean is used automatically (only works when there is exactly one Job
 *   bean; use setJob() if there are multiple).
 *
 * JobLauncherTestUtils.launchStep(stepName):
 *   Runs a single step in isolation — useful for testing step metrics independently.
 *   Creates its own JobExecution with empty JobParameters.
 *
 * No @Transactional on the test class — Spring Batch manages its own transactions per chunk.
 * Rolling back the test transaction would conflict with Batch's commit-per-chunk model.
 */
@SpringBatchTest
@SpringBootTest
class ProductImportJobTest {

    @Autowired JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils;
    @Autowired ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // Clean batch metadata so the same JobParameters can be reused across test methods
        jobRepositoryTestUtils.removeJobExecutions();
        productRepository.deleteAll();
    }

    // ── Full job ──────────────────────────────────────────────────────────────

    @Test
    void full_job_completes_successfully() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob(defaultJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
    }

    @Test
    void full_job_writes_all_valid_products_to_database() throws Exception {
        jobLauncherTestUtils.launchJob(defaultJobParameters());

        // CSV: 7 data rows. Row 7 has blank name → processor returns null → filtered.
        // 6 valid rows are written to the database.
        assertThat(productRepository.count()).isEqualTo(6);
    }

    // ── Step metrics ──────────────────────────────────────────────────────────

    @Test
    void step_reports_correct_read_write_and_filter_counts() throws Exception {
        // launchStep runs the step in isolation with empty JobParameters.
        // The @StepScope reader defaults to 'products.csv' (via the ?: fallback).
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("importStep");
        StepExecution stepExecution = jobExecution.getStepExecutions().stream()
                .filter(se -> se.getStepName().equals("importStep"))
                .findFirst()
                .orElseThrow();

        // CSV has 7 data rows
        assertThat(stepExecution.getReadCount()).isEqualTo(7);
        // Processor returns null for the blank-name row → filterCount (not skipCount)
        assertThat(stepExecution.getFilterCount()).isEqualTo(1);
        // 6 rows passed through and written
        assertThat(stepExecution.getWriteCount()).isEqualTo(6);
        // No exceptions were thrown → no skips
        assertThat(stepExecution.getSkipCount()).isEqualTo(0);
    }

    // ── Data quality ──────────────────────────────────────────────────────────

    @Test
    void processor_normalises_all_categories_to_uppercase() throws Exception {
        jobLauncherTestUtils.launchJob(defaultJobParameters());

        productRepository.findAll().forEach(p ->
                assertThat(p.getCategory())
                        .as("category for product '%s'", p.getName())
                        .isEqualTo(p.getCategory().toUpperCase()));
    }

    @Test
    void processor_trims_whitespace_from_product_names() throws Exception {
        jobLauncherTestUtils.launchJob(defaultJobParameters());

        productRepository.findAll().forEach(p ->
                assertThat(p.getName())
                        .as("name should have no leading/trailing whitespace")
                        .isEqualTo(p.getName().strip()));
    }

    // ── Re-runnability ────────────────────────────────────────────────────────

    @Test
    void job_with_different_run_id_creates_new_instance_and_completes() throws Exception {
        // Spring Batch identifies a job instance by (jobName + JobParameters).
        // The same parameters on a COMPLETED job → NOOP (not re-run).
        // A unique run.id forces a new instance, allowing idempotent re-runs.
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addString("input.file", "products.csv")
                .addLong("run.id", 1L)
                .toJobParameters());

        productRepository.deleteAll();
        jobRepositoryTestUtils.removeJobExecutions();

        JobExecution secondRun = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addString("input.file", "products.csv")
                .addLong("run.id", 2L)
                .toJobParameters());

        assertThat(secondRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(productRepository.count()).isEqualTo(6);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JobParameters defaultJobParameters() {
        return new JobParametersBuilder()
                .addString("input.file", "products.csv")
                .addLong("run.id", 1L)
                .toJobParameters();
    }
}
