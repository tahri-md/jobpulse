package com.jobpulse.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jobpulse.config.CustomDetailsService;
import com.jobpulse.config.JwtAuthFilter;
import com.jobpulse.service.JobService;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private CustomDetailsService customDetailsService;

    @Test
    void createJobFullRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "nightly-report",
                                  "jobType": "LOG",
                                  "payload": "hello",
                                  "schedule": {
                                    "type": "CRON",
                                    "cronExpression": "0 0 * * * *"
                                  },
                                  "maxRetries": 3
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void listJobsRequiresAuthentication() throws Exception {
      mockMvc.perform(get("/api/v1/jobs"))
          .andExpect(status().isUnauthorized());
    }
}
