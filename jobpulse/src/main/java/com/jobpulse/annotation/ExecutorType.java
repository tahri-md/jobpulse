package com.jobpulse.annotation;

import com.jobpulse.dto.request.JobRequestDTO.JobType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExecutorType {
  JobType value();
}
