package com.jobpulse.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.request.JobRequestDTO.JobType;

@Component
public class JobExecutorFactory {

    private final Map<JobType, JobExecutor> executors;

    public JobExecutorFactory(List<JobExecutor> executorList) {
        executors = executorList.stream()
                .collect(Collectors.toMap(
                        e -> e.getClass().getAnnotation(ExecutorType.class).value(),
                        e -> e
                ));
    }

    public JobExecutor get(JobType type) {
        return executors.get(type);
    }
}

