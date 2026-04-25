package com.javatraining.performance.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired TaskService taskService;

    @Test
    void submit_returns_202_accepted_with_task_id_and_location_header() throws Exception {
        mockMvc.perform(post("/tasks"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.taskId").value(notNullValue()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(header().string("Location",
                        matchesPattern("/tasks/[0-9a-f-]+")));
    }

    @Test
    void task_status_transitions_to_done_after_virtual_thread_completes() throws Exception {
        // Submit the task
        var result = mockMvc.perform(post("/tasks"))
                .andExpect(status().isAccepted())
                .andReturn();

        String taskId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString())
                .get("taskId").asText();

        // The task sleeps 100 ms on a virtual thread - wait 3× longer for CI headroom
        Thread.sleep(400);

        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }
}
