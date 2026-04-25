package com.javatraining.batch.batch;

import com.javatraining.batch.model.Product;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.javatraining.batch.repository.ProductRepository;

/**
 * Batch job configuration.
 *
 * Spring Boot 3.x conventions:
 *   - NO @EnableBatchProcessing - Spring Boot auto-configures JobRepository, JobLauncher,
 *     JobExplorer automatically. Adding @EnableBatchProcessing disables the auto-config.
 *   - Use new JobBuilder(name, jobRepository) - JobBuilderFactory is deprecated in Batch 5.
 *   - Use new StepBuilder(name, jobRepository) - StepBuilderFactory is deprecated in Batch 5.
 *   - PlatformTransactionManager is injected directly - no longer a JobBuilderFactory detail.
 *
 * Chunk-oriented processing flow (per chunk of size N):
 *
 *   ┌─────────────────────────────────────────────────────────────────┐
 *   │  BEGIN TRANSACTION                                              │
 *   │  for i in 0..chunkSize:                                         │
 *   │      item = reader.read()    ← null signals end of input        │
 *   │      if item == null: break                                      │
 *   │      result = processor.process(item)  ← null = filter          │
 *   │      if result != null: chunkBuffer.add(result)                 │
 *   │  writer.write(chunkBuffer)                                      │
 *   │  COMMIT                                                         │
 *   └─────────────────────────────────────────────────────────────────┘
 *
 * On failure within a chunk: the whole chunk is rolled back and retried
 * item-by-item to identify and skip the bad item (if skip is configured).
 */
@Configuration
public class ProductImportJobConfig {

    /**
     * FlatFileItemReader - reads one line at a time, tokenizes it, maps to ProductCsvRow.
     *
     * @StepScope: bean is created fresh for each StepExecution (not at application startup).
     *   Required when the bean uses job/step parameters via @Value("#{jobParameters[...]}").
     *   The bean is a scoped proxy at startup; the real instance is created when the step runs.
     *
     * @Value("#{jobParameters['input.file'] ?: 'products.csv'}"):
     *   Late binding - the SpEL expression is evaluated when the step scope activates, not
     *   when the application context starts. The ?: operator provides a fallback so tests
     *   can launch the step without explicitly providing the parameter.
     */
    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public FlatFileItemReader<ProductCsvRow> productReader(
            @Value("#{jobParameters['input.file'] ?: 'products.csv'}") String fileName) {
        return new FlatFileItemReaderBuilder<ProductCsvRow>()
                .name("productReader")
                .resource(new ClassPathResource(fileName))
                .delimited()
                .names("name", "category", "price")   // column names - must match CSV header
                .targetType(ProductCsvRow.class)       // BeanWrapperFieldSetMapper target
                .linesToSkip(1)                        // skip the header row
                .build();
    }

    /**
     * RepositoryItemWriter - delegates to ProductRepository.save() for each item in the chunk.
     * Participates in the chunk's transaction automatically (same EntityManager / tx context).
     */
    @Bean
    public RepositoryItemWriter<Product> productWriter(ProductRepository productRepository) {
        return new RepositoryItemWriterBuilder<Product>()
                .repository(productRepository)
                .methodName("save")
                .build();
    }

    /**
     * Step - the unit of work.
     *
     * chunk(10, transactionManager):
     *   Read up to 10 items, process them, write them all in one transaction.
     *   Larger chunk size → fewer transactions → faster. Trade-off: larger rollback on failure.
     *
     * faultTolerant():
     *   Enables skip and retry policies. Without this, any exception fails the step immediately.
     *
     * skip(IllegalArgumentException.class).skipLimit(3):
     *   Tolerate up to 3 items that throw IllegalArgumentException during processing.
     *   Spring Batch re-runs the failing chunk item-by-item to isolate and skip the bad one.
     *   Beyond the limit, the exception propagates and the step fails.
     */
    @Bean
    public Step importStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           FlatFileItemReader<ProductCsvRow> productReader,
                           ProductItemProcessor productItemProcessor,
                           RepositoryItemWriter<Product> productWriter,
                           ImportStepListener importStepListener) {
        return new StepBuilder("importStep", jobRepository)
                .<ProductCsvRow, Product>chunk(10, transactionManager)
                .reader(productReader)
                .processor(productItemProcessor)
                .writer(productWriter)
                .faultTolerant()
                .skip(IllegalArgumentException.class)
                .skipLimit(3)
                .listener(importStepListener)
                .build();
    }

    /**
     * Job - the top-level batch unit.
     *
     * A Job is a container for Steps. Steps execute in the order declared.
     * The Job tracks its own execution state in the BATCH_JOB_INSTANCE and
     * BATCH_JOB_EXECUTION metadata tables.
     *
     * Re-runnability: Spring Batch identifies a job instance by (jobName + JobParameters).
     * The same parameters = same job instance. A COMPLETED instance is NOT re-run by default.
     * Use a unique parameter (e.g. timestamp or run.id) to force a new instance each run.
     */
    @Bean
    public Job productImportJob(JobRepository jobRepository,
                                Step importStep,
                                JobCompletionListener jobCompletionListener) {
        return new JobBuilder("productImportJob", jobRepository)
                .listener(jobCompletionListener)
                .start(importStep)
                .build();
    }
}
