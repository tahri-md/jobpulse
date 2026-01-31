package com.jobpulse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.request.JobRequestDTO.JobType;
import com.jobpulse.model.Job;

@Component
@ExecutorType(JobType.LOG)
public class LogJobExecutor implements JobExecutor {

    @Autowired
    ObjectMapper mapper;
    @Override
    public void execute(Job job) {
        try {
            JsonNode node = mapper.readTree(job.getPayload());
            System.out.println("[JOB LOG] " + node.get("message").asText());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
