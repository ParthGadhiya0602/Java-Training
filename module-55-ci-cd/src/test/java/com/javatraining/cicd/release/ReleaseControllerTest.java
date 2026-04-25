package com.javatraining.cicd.release;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReleaseControllerTest {

    @Autowired MockMvc            mockMvc;
    @Autowired ReleaseRepository  releaseRepository;

    @BeforeEach
    void setUp() {
        releaseRepository.deleteAll();
    }

    @Test
    void post_with_valid_semver_returns_201_with_location() throws Exception {
        mockMvc.perform(post("/releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"backend-service","version":"2.0.0","releasedAt":"2024-03-15"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value("2.0.0"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(header().string("Location", containsString("/releases/")));
    }

    @Test
    void post_with_invalid_semver_returns_400() throws Exception {
        mockMvc.perform(post("/releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"backend-service","version":"v2.0","releasedAt":"2024-03-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("semantic versioning")));
    }
}
