package com.jobpulse.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.dto.request.HttpJobPayload;
import com.jobpulse.model.Job;

public class HttpJobExecutor implements JobExecutor {
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    WebClient webClient;

    @Override
    public void execute(Job job) throws Exception {
       HttpJobPayload payload = objectMapper.readValue(job.getPayload(), HttpJobPayload.class);
         WebClient.RequestBodySpec request = webClient
            .method(HttpMethod.valueOf(payload.method))
            .uri(payload.url);

        if (payload.headers != null) {
            request.headers(h -> h.setAll(payload.headers));
        }

        WebClient.ResponseSpec response =
            payload.body != null
                ? request.bodyValue(payload.body).retrieve()
                : request.retrieve();

        response
            .toBodilessEntity()
            .block(Duration.ofSeconds(
                payload.timeoutSeconds != null ? payload.timeoutSeconds : 10
            ));
    }
    }
    

