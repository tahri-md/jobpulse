package com.jobpulse.service;

import com.jobpulse.model.Job;

public interface JobExecutor {
    void execute(Job job) throws Exception;
}
