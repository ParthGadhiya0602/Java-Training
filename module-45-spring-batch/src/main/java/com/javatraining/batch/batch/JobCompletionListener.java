package com.javatraining.batch.batch;

import com.javatraining.batch.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * JobExecutionListener — called before and after the entire Job.
 *
 * beforeJob: runs after the job is started but before the first Step executes.
 *   Good for: validating preconditions, logging parameters, opening resources.
 *
 * afterJob: runs after all Steps complete (success or failure).
 *   Good for: summary logging, sending notifications, closing resources.
 *   The JobExecution.getStatus() tells you whether the job succeeded or failed.
 */
@Component
public class JobCompletionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionListener.class);

    private final ProductRepository productRepository;

    public JobCompletionListener(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job '{}' starting — parameters: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job '{}' completed — total products in DB: {}",
                    jobExecution.getJobInstance().getJobName(),
                    productRepository.count());
        } else {
            log.error("Job '{}' ended with status: {} — failures: {}",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus(),
                    jobExecution.getAllFailureExceptions());
        }
    }
}
