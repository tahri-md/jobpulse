package com.jobpulse.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.jobpulse.model.Job;
import com.jobpulse.model.JobHistory;
import java.util.List;


@Repository
public interface JobHistoryRepository extends JpaRepository<JobHistory, Long> {
    boolean existsByJob(Job job);
    
    List<JobHistory> findByJobOrderByRunTimeDesc(Job job);
    
    @Query("SELECT jh FROM JobHistory jh WHERE jh.job = :job ORDER BY jh.runTime DESC LIMIT 1")
    Optional<JobHistory> findLatestByJob(Job job);
}
