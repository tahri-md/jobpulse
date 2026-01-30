package com.jobpulse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.DeadLetterJob;
@Repository
public interface DeadLetterJobRepository extends JpaRepository<DeadLetterJob,Long> {
    
}
