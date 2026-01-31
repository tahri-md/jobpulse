package com.jobpulse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.request.JobRequestDTO.JobType;
import com.jobpulse.model.Job;

@Component
@ExecutorType(JobType.SCRIPT)
public class ScriptJobExecutor implements JobExecutor {

    @Autowired
    ObjectMapper mapper;
    @Override
    public void execute(Job job) {
        try {
            JsonNode node = mapper.readTree(job.getPayload());
            String command = node.get("command").asText();

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
