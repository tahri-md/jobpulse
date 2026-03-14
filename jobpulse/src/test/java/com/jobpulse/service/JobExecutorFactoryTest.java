package com.jobpulse.service;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.jobpulse.annotation.ExecutorType;
import com.jobpulse.dto.request.JobRequestDTO.JobType;
import com.jobpulse.model.Job;

class JobExecutorFactoryTest {

    @Test
    void returnsExecutorMappedByAnnotation() {
        JobExecutor httpExecutor = new HttpCallExecutorStub();
        JobExecutor logExecutor = new LogExecutorStub();

        JobExecutorFactory factory = new JobExecutorFactory(List.of(httpExecutor, logExecutor));

        assertSame(httpExecutor, factory.get(JobType.HTTP_CALL));
        assertSame(logExecutor, factory.get(JobType.LOG));
    }

    @ExecutorType(JobType.HTTP_CALL)
    private static class HttpCallExecutorStub implements JobExecutor {
        @Override
        public void execute(Job job) {
        }
    }

    @ExecutorType(JobType.LOG)
    private static class LogExecutorStub implements JobExecutor {
        @Override
        public void execute(Job job) {
        }
    }
}
