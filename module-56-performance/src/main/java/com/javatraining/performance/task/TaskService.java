package com.javatraining.performance.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Submits work to a virtual-thread-per-task executor.
 *
 * Each submitted task gets its own virtual thread. Virtual threads are cheap —
 * the JVM can schedule millions of them on a small set of carrier (OS) threads.
 * When a virtual thread blocks (sleep, I/O, lock) it is unmounted from its carrier
 * immediately, freeing the carrier to run another virtual thread.
 */
@Slf4j
@Service
public class TaskService {

    private final ConcurrentMap<String, TaskStatus> tasks = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public String submit(String payload) {
        String taskId = UUID.randomUUID().toString();
        tasks.put(taskId, TaskStatus.PENDING);

        executor.submit(() -> {
            tasks.put(taskId, TaskStatus.RUNNING);
            log.debug("Running task {} on thread {} (virtual={})",
                    taskId, Thread.currentThread().getName(),
                    Thread.currentThread().isVirtual());

            // Simulate I/O-bound work — a virtual thread unmounts here
            Thread.sleep(100);

            tasks.put(taskId, TaskStatus.DONE);
            log.debug("Completed task {}", taskId);
            return null;
        });

        return taskId;
    }

    public Optional<TaskStatus> status(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public enum TaskStatus {
        PENDING, RUNNING, DONE
    }
}
