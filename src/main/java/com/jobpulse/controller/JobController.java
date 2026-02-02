package com.jobpulse.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.jobpulse.dto.request.JobRequest;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.dto.response.JobResponse;
import com.jobpulse.model.User;
import com.jobpulse.service.JobService;
import com.jobpulse.service.JobService.DeadLetterJobResponse;
import com.jobpulse.service.JobService.JobHistoryResponse;
import com.jobpulse.service.JobService.JobStatsResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/jobs")
@Validated
@Slf4j
public class JobController {

    @Autowired
    private JobService jobService;

  
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        log.info("Creating simple job: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
    }


    @PostMapping("/full")
    public ResponseEntity<JobResponse> createJobFull(
            @RequestBody JobRequestDTO request,
            @AuthenticationPrincipal User user) {
        log.info("Creating full job: {} for user: {}", request.getName(), user.getId());
        request.setOwnerId(user.getId());
        jobService.createJobFull(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

   
    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        log.debug("Fetching all jobs");
        return ResponseEntity.ok(jobService.getJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable long id) {
        log.debug("Fetching job with ID: {}", id);
        return ResponseEntity.ok(jobService.getJob(id));
    }

 
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable long id) {
        log.info("Deleting job with ID: {}", id);
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

  
    @PostMapping("/dead-letter/{id}/replay")
    public ResponseEntity<JobResponse> replayDeadJob(@PathVariable Long id) {
        log.info("Replaying dead letter job with ID: {}", id);
        return ResponseEntity.ok(jobService.replayDeadJob(id));
    }

  
    @GetMapping("/dead-letter")
    public ResponseEntity<List<DeadLetterJobResponse>> getDeadLetterJobs() {
        log.debug("Fetching all dead letter jobs");
        return ResponseEntity.ok(jobService.getDeadLetterJobs());
    }

   
    @GetMapping("/{id}/history")
    public ResponseEntity<List<JobHistoryResponse>> getJobHistory(@PathVariable long id) {
        log.debug("Fetching history for job with ID: {}", id);
        return ResponseEntity.ok(jobService.getJobHistory(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<JobStatsResponse> getJobStats() {
        log.debug("Fetching job statistics");
        return ResponseEntity.ok(jobService.getJobStats());
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<JobResponse> pauseJob(@PathVariable long id) {
        log.info("Pausing job with ID: {}", id);
        return ResponseEntity.ok(jobService.pauseJob(id));
    }

  
    @PutMapping("/{id}/resume")
    public ResponseEntity<JobResponse> resumeJob(@PathVariable long id) {
        log.info("Resuming job with ID: {}", id);
        return ResponseEntity.ok(jobService.resumeJob(id));
    }
}
