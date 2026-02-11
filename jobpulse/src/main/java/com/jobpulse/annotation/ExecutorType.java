package com.jobpulse.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jobpulse.dto.request.JobRequestDTO.JobType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExecutorType {
    JobType value();
}
