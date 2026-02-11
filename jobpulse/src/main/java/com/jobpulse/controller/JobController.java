package com.jobpulse.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.dto.response.JobResponse;
import com.jobpulse.model.User;
import com.jobpulse.service.JobService;
import com.jobpulse.service.JobService.DeadLetterJobResponse;
import com.jobpulse.service.JobService.JobHistoryResponse;
import com.jobpulse.service.JobService.JobStatsResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/jobs")
@Slf4j
public class JobController {

    @Autowired
    private JobService jobService;

  
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
    public ResponseEntity<List<JobResponse>> getAllJobs(@AuthenticationPrincipal User user) {
        log.debug("Fetching jobs for user: {}", user.getId());
        return ResponseEntity.ok(jobService.getJobs(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable long id, @AuthenticationPrincipal User user) {
        log.debug("Fetching job with ID: {} for user: {}", id, user.getId());
        return ResponseEntity.ok(jobService.getJob(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable long id, @AuthenticationPrincipal User user) {
        log.info("Deleting job with ID: {} for user: {}", id, user.getId());
        jobService.deleteJob(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dead-letter/{id}/replay")
    public ResponseEntity<JobResponse> replayDeadJob(@PathVariable Long id, @AuthenticationPrincipal User user) {
        log.info("Replaying dead letter job with ID: {} for user: {}", id, user.getId());
        return ResponseEntity.ok(jobService.replayDeadJob(id, user));
    }

    @GetMapping("/dead-letter")
    public ResponseEntity<List<DeadLetterJobResponse>> getDeadLetterJobs(@AuthenticationPrincipal User user) {
        log.debug("Fetching dead letter jobs for user: {}", user.getId());
        return ResponseEntity.ok(jobService.getDeadLetterJobs(user));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<JobHistoryResponse>> getJobHistory(@PathVariable long id, @AuthenticationPrincipal User user) {
        log.debug("Fetching history for job with ID: {} for user: {}", id, user.getId());
        return ResponseEntity.ok(jobService.getJobHistory(id, user));
    }

    @GetMapping("/stats")
    public ResponseEntity<JobStatsResponse> getJobStats(@AuthenticationPrincipal User user) {
        log.debug("Fetching job statistics for user: {}", user.getId());
        return ResponseEntity.ok(jobService.getJobStats(user));
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<JobResponse> pauseJob(@PathVariable long id, @AuthenticationPrincipal User user) {
        log.info("Pausing job with ID: {} for user: {}", id, user.getId());
        return ResponseEntity.ok(jobService.pauseJob(id, user));
    }

    @PutMapping("/{id}/resume")
    public ResponseEntity<JobResponse> resumeJob(@PathVariable long id, @AuthenticationPrincipal User user) {
        log.info("Resuming job with ID: {} for user: {}", id, user.getId());
        return ResponseEntity.ok(jobService.resumeJob(id, user));
    }
}
