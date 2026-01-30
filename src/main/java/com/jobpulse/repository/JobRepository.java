package com.jobpulse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.Job;
@Repository
public interface JobRepository extends JpaRepository<Job,Long> {
    
}
