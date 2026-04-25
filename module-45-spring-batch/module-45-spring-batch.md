---
title: "Module 45 — Spring Batch"
parent: "Phase 5 — Spring Ecosystem"
nav_order: 45
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-45-spring-batch/src){: .btn .btn-outline }

# Module 45 — Spring Batch

## Overview

Spring Batch is a framework for processing large volumes of data reliably.
It structures work as **Jobs** composed of **Steps**, each Step reading,
transforming, and writing records in fixed-size **chunks** — so a failure
mid-way through 10 million rows means at most one chunk is re-processed,
not the entire file.

---

## 1. Architecture

```
Job
└── Step  (can have many; run in sequence or conditionally)
    ├── ItemReader     — reads ONE item at a time; null signals end-of-input
    ├── ItemProcessor  — transforms the item; null means filter (don't write)
    └── ItemWriter     — receives the full chunk buffer and writes it atomically
```

**Chunk-oriented processing flow (per chunk of size N):**

```
BEGIN TRANSACTION
  for i in 0..chunkSize:
      item = reader.read()           ← null → break
      result = processor.process(item)
      if result != null: buffer.add(result)
  writer.write(buffer)
COMMIT
```

One transaction per chunk. A failure rolls back only the current chunk, not
previous ones. With `faultTolerant().skip(...)` Spring Batch re-runs the
failing chunk item-by-item to isolate and skip the bad item.

---

## 2. Spring Boot 3.x conventions

```java
// Spring Boot auto-configures JobRepository, JobLauncher, and JobExplorer.
// @EnableBatchProcessing DISABLES that auto-config — do NOT use it.

@Bean
public Job productImportJob(JobRepository jobRepository, Step importStep,
                             JobCompletionListener listener) {
    return new JobBuilder("productImportJob", jobRepository)  // NOT JobBuilderFactory
            .listener(listener)
            .start(importStep)
            .build();
}

@Bean
public Step importStep(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager,
                       FlatFileItemReader<ProductCsvRow> reader,
                       ProductItemProcessor processor,
                       RepositoryItemWriter<Product> writer,
                       ImportStepListener stepListener) {
    return new StepBuilder("importStep", jobRepository)  // NOT StepBuilderFactory
            .<ProductCsvRow, Product>chunk(10, transactionManager)
            .reader(reader).processor(processor).writer(writer)
            .faultTolerant()
            .skip(IllegalArgumentException.class).skipLimit(3)
            .listener(stepListener)
            .build();
}
```

`JobBuilderFactory` / `StepBuilderFactory` are deprecated in Spring Batch 5.
Use `new JobBuilder(name, jobRepository)` / `new StepBuilder(name, jobRepository)` directly.

---

## 3. `FlatFileItemReader` and `@StepScope`

```java
@Bean
@StepScope   // bean is created per StepExecution, not at application startup
public FlatFileItemReader<ProductCsvRow> productReader(
        @Value("#{jobParameters['input.file'] ?: 'products.csv'}") String fileName) {
    return new FlatFileItemReaderBuilder<ProductCsvRow>()
            .name("productReader")
            .resource(new ClassPathResource(fileName))
            .delimited()
            .names("name", "category", "price")   // must match CSV header
            .targetType(ProductCsvRow.class)       // BeanWrapperFieldSetMapper
            .linesToSkip(1)                        // skip header row
            .build();
}
```

**`@StepScope` is required** when a bean reads from `jobParameters` or
`stepExecutionContext` via `@Value` SpEL expressions. The bean is a scoped
proxy at startup; the real instance is created when the step runs.

**Elvis fallback `?: 'products.csv'`** — evaluated when the step scope activates.
Allows `launchStep("importStep")` in tests without passing any parameters.

**`ProductCsvRow` must be a mutable JavaBean** (no-arg constructor + setters)
because `BeanWrapperFieldSetMapper` calls setters by reflection:

```java
@Data
@NoArgsConstructor
public class ProductCsvRow {
    private String name;
    private String category;
    private BigDecimal price;
}
```

---

## 4. `ItemProcessor` — filter vs skip

The processor is the data quality gate. Two distinct outcomes:

| Processor returns | Spring Batch action | Counter incremented |
|---|---|---|
| a non-null item | passes item to writer | `writeCount` |
| `null` | silently drops the item | `filterCount` |
| throws an exception | rolls back chunk; re-runs item-by-item; if `skip` configured, item is skipped | `processSkipCount` (within `skipCount`) |

```java
@Component
public class ProductItemProcessor implements ItemProcessor<ProductCsvRow, Product> {

    @Override
    public Product process(ProductCsvRow row) {
        if (row.getName() == null || row.getName().isBlank()) {
            return null;               // filter — increments filterCount, NOT skipCount
        }
        if (row.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price: " + row.getPrice());
            // increments processSkipCount if skip(IllegalArgumentException.class) is configured
        }
        return Product.builder()
                .name(row.getName().strip())
                .category(row.getCategory().toUpperCase())
                .price(row.getPrice())
                .build();
    }
}
```

---

## 5. `RepositoryItemWriter`

```java
@Bean
public RepositoryItemWriter<Product> productWriter(ProductRepository repo) {
    return new RepositoryItemWriterBuilder<Product>()
            .repository(repo)
            .methodName("save")   // calls repo.save(item) for each item in the chunk
            .build();
}
```

The writer participates in the chunk's transaction automatically — same
`EntityManager` / transaction context.

---

## 6. Listeners

### `JobExecutionListener`

Runs before/after the entire job. Injected via `.listener(...)` on `JobBuilder`.

```java
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job '{}' starting — parameters: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Completed — products in DB: {}", repository.count());
        } else {
            log.error("Failed: {}", jobExecution.getAllFailureExceptions());
        }
    }
}
```

### `StepExecutionListener`

Runs before/after each step. Injected via `.listener(...)` on `StepBuilder`.
Returning `null` from `afterStep` keeps the step's existing exit status;
returning a custom `ExitStatus` overrides it (useful for conditional flows).

```java
@Component
public class ImportStepListener implements StepExecutionListener {

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("read={}, written={}, filtered={}, skipped={}, rollbacks={}",
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getFilterCount(),
                stepExecution.getSkipCount(),
                stepExecution.getRollbackCount());
        return null;   // keep existing exit status
    }
}
```

**`StepExecution` counters:**

| Counter | Meaning |
|---|---|
| `readCount` | items successfully read |
| `writeCount` | items successfully written |
| `filterCount` | items returning null from processor |
| `readSkipCount` | items skipped due to read exception |
| `processSkipCount` | items skipped due to processor exception |
| `writeSkipCount` | items skipped due to writer exception |
| `skipCount` | sum of all three skip counters |
| `rollbackCount` | chunk transaction rollbacks |

---

## 7. Re-runnability

Spring Batch identifies a **job instance** by `(jobName + JobParameters)`.
The same parameters on a `COMPLETED` job → the job is not re-run.

To allow idempotent re-runs, include a unique parameter such as `run.id`
or a timestamp:

```java
JobParameters params = new JobParametersBuilder()
        .addString("input.file", "products.csv")
        .addLong("run.id", System.currentTimeMillis())
        .toJobParameters();
```

Disable automatic job launch on startup while keeping the auto-configuration:

```properties
spring.batch.job.enabled=false
```

---

## 8. Partitioned steps (local parallelism)

Partitioning fans a single step out to N worker threads, each processing a
non-overlapping slice of data. `RangePartitioner` divides an ID range:

```java
public class RangePartitioner implements Partitioner {

    private final int totalItems;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int size = Math.max(1, totalItems / gridSize);
        Map<String, ExecutionContext> result = new LinkedHashMap<>();
        for (int i = 0; i < gridSize; i++) {
            int min = i * size;
            int max = (i == gridSize - 1) ? totalItems - 1 : min + size - 1;
            ExecutionContext ctx = new ExecutionContext();
            ctx.putInt("minValue", min);
            ctx.putInt("maxValue", max);
            result.put("partition" + i, ctx);
        }
        return result;
    }
}
```

Wire it into the step builder:

```java
new StepBuilder("partitionedStep", jobRepository)
        .partitioner("workerStep", new RangePartitioner(totalRows))
        .step(workerStep)
        .gridSize(4)
        .taskExecutor(new SimpleAsyncTaskExecutor())
        .build();
```

The worker step reads `minValue`/`maxValue` from its own `ExecutionContext`
via `@StepScope`:

```java
@Bean
@StepScope
public JdbcCursorItemReader<Product> partitionedReader(
        @Value("#{stepExecutionContext['minValue']}") int minValue,
        @Value("#{stepExecutionContext['maxValue']}") int maxValue) {
    return new JdbcCursorItemReaderBuilder<Product>()
            .sql("SELECT * FROM products WHERE id BETWEEN ? AND ?")
            .queryArguments(minValue, maxValue)
            ...build();
}
```

Remote partitioning (distributing work across JVMs) requires Spring Batch Integration.

---

## 9. Testing with `@SpringBatchTest`

`@SpringBatchTest` adds to the application context:

| Bean | Purpose |
|---|---|
| `JobLauncherTestUtils` | launch full jobs or individual steps programmatically |
| `JobRepositoryTestUtils` | clean up `BATCH_*` metadata tables between tests |
| `StepScopeTestExecutionListener` | activates step scope for `@StepScope` bean injection |
| `JobScopeTestExecutionListener` | activates job scope for `@JobScope` bean injection |

Must be combined with `@SpringBootTest` (or `@ContextConfiguration`) — `@SpringBatchTest`
alone does not load the application context.

```java
@SpringBatchTest
@SpringBootTest
class ProductImportJobTest {

    @Autowired JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils;
    @Autowired ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();  // reuse same JobParameters across tests
        productRepository.deleteAll();
    }

    @Test
    void full_job_completes_successfully() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob(defaultJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void step_reports_correct_read_write_and_filter_counts() throws Exception {
        // launchStep runs the step in isolation; @StepScope reader defaults to 'products.csv'
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("importStep");
        StepExecution stepExecution = jobExecution.getStepExecutions().stream()
                .filter(se -> se.getStepName().equals("importStep"))
                .findFirst()
                .orElseThrow();

        assertThat(stepExecution.getReadCount()).isEqualTo(7);    // CSV rows
        assertThat(stepExecution.getFilterCount()).isEqualTo(1);  // blank-name row
        assertThat(stepExecution.getWriteCount()).isEqualTo(6);   // written to DB
        assertThat(stepExecution.getSkipCount()).isEqualTo(0);
    }

    private JobParameters defaultJobParameters() {
        return new JobParametersBuilder()
                .addString("input.file", "products.csv")
                .addLong("run.id", 1L)
                .toJobParameters();
    }
}
```

**Why no `@Transactional` on the test class?**
Spring Batch commits one transaction per chunk. Rolling back the test
transaction would conflict with those chunk commits and leave metadata tables
in an inconsistent state. Clean up with `JobRepositoryTestUtils.removeJobExecutions()`
and `repository.deleteAll()` in `@BeforeEach` instead.

**`StepScopeTestExecutionListener` method-scanning caveat (Spring Batch 5.x):**
The listener scans the test class for any method returning `StepExecution` to use
as a factory for step scope activation. Do not declare helper methods with that
return type — inline the stream logic instead.

---

## Key takeaways

- Spring Batch structures work as Jobs → Steps → chunks; each chunk is one
  transaction — failure rolls back only that chunk, not the whole job
- Do not use `@EnableBatchProcessing` in Spring Boot 3.x — it disables the
  auto-configuration. Use `JobBuilder`/`StepBuilder` directly (factory classes deprecated)
- `@StepScope` enables per-step bean creation and late-binding of `jobParameters` /
  `stepExecutionContext` via SpEL; the Elvis `?:` fallback lets tests call
  `launchStep()` without providing parameters
- Processor returning `null` increments `filterCount` (silent drop);
  throwing an exception increments `skipCount` (when `faultTolerant().skip()` is configured)
- `RangePartitioner` divides data into ID ranges for local parallel processing;
  each partition's `minValue`/`maxValue` is injected via `@StepScope`
- Test with `@SpringBatchTest + @SpringBootTest`; `JobLauncherTestUtils.launchJob()`
  runs the full job, `launchStep("name")` runs one step in isolation
- Clean up between tests with `jobRepositoryTestUtils.removeJobExecutions()` and
  `repository.deleteAll()` — no `@Transactional` on the test class
{% endraw %}
