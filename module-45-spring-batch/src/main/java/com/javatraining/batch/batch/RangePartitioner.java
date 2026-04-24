package com.javatraining.batch.batch;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RangePartitioner — divides a dataset into N non-overlapping ID ranges.
 *
 * Used with partitioned steps to process large tables in parallel:
 *
 *   Step builder:
 *     new StepBuilder("partitionedStep", jobRepository)
 *         .partitioner("workerStep", new RangePartitioner(totalRows))
 *         .step(workerStep)               // the step to fan out
 *         .gridSize(4)                    // number of partitions (threads)
 *         .taskExecutor(taskExecutor())   // SimpleAsyncTaskExecutor for local parallelism
 *         .build();
 *
 *   Worker step (must be @StepScope to read the partition context):
 *     @StepScope
 *     @Bean
 *     public JdbcCursorItemReader<Product> partitionedReader(
 *             @Value("#{stepExecutionContext['minValue']}") int minValue,
 *             @Value("#{stepExecutionContext['maxValue']}") int maxValue) {
 *         return new JdbcCursorItemReaderBuilder<Product>()
 *             .sql("SELECT * FROM products WHERE id BETWEEN ? AND ?")
 *             .queryArguments(minValue, maxValue)
 *             ...build();
 *     }
 *
 * Each partition receives its own ExecutionContext with minValue/maxValue,
 * injected into the worker step's @StepScope beans via #{stepExecutionContext[...]}.
 *
 * This class is included as a reference implementation for local (single-JVM) partitioning.
 * Remote partitioning (distributing work across JVMs) requires Spring Batch Integration.
 */
public class RangePartitioner implements Partitioner {

    private final int totalItems;

    public RangePartitioner(int totalItems) {
        this.totalItems = totalItems;
    }

    /**
     * Divides [0, totalItems) into gridSize ranges.
     * Returns a map of partition name → ExecutionContext for each partition.
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int partitionSize = Math.max(1, totalItems / gridSize);
        Map<String, ExecutionContext> result = new LinkedHashMap<>();

        for (int i = 0; i < gridSize; i++) {
            int minValue = i * partitionSize;
            int maxValue = (i == gridSize - 1) ? totalItems - 1 : minValue + partitionSize - 1;

            ExecutionContext context = new ExecutionContext();
            context.putInt("minValue", minValue);
            context.putInt("maxValue", maxValue);
            result.put("partition" + i, context);
        }
        return result;
    }
}
