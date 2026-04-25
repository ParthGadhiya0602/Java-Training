package com.javatraining.performance.task;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<Map<String, String>> submit() {
        String taskId = taskService.submit("work");
        return ResponseEntity
                .accepted()
                .location(URI.create("/tasks/" + taskId))
                .body(Map.of("taskId", taskId, "status", "PENDING"));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, String>> status(@PathVariable String taskId) {
        return taskService.status(taskId)
                .map(s -> ResponseEntity.ok(Map.of("taskId", taskId, "status", s.name())))
                .orElse(ResponseEntity.notFound().build());
    }
}
