package com.javatraining.batch.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * StepExecutionListener - called before and after each Step.
 *
 * StepExecution carries per-step counters updated by Spring Batch:
 *   readCount        - items successfully read (does not count read skips)
 *   writeCount       - items successfully written
 *   filterCount      - items returned null by the processor (silently dropped)
 *   readSkipCount    - items skipped due to read exceptions
 *   processSkipCount - items skipped due to processor exceptions
 *   writeSkipCount   - items skipped due to writer exceptions
 *   skipCount        - total of all three skip counters above
 *   rollbackCount    - number of chunk transaction rollbacks
 *
 * afterStep returns ExitStatus - return null to keep the step's existing status,
 * or return a custom ExitStatus to override it (e.g. for conditional flows).
 */
@Component
public class ImportStepListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ImportStepListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Step '{}' starting", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Step '{}' finished - read={}, written={}, filtered={}, skipped={}, rollbacks={}",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getFilterCount(),
                stepExecution.getSkipCount(),
                stepExecution.getRollbackCount());
        return null; // keep existing exit status
    }
}
