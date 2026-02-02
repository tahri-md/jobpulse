package com.jobpulse.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingInitializer.NonAggregatedIdentifierMappingInitializerData;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.jobpulse.dto.others.JobFailureReason;
import com.jobpulse.dto.request.JobRequest;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.dto.request.ScheduleDTO;
import com.jobpulse.dto.request.ScheduleDTO.ScheduleType;
import com.jobpulse.dto.response.JobResponse;
import com.jobpulse.exception.JobExecutionException;
import com.jobpulse.exception.NonRetryableJobException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.model.DeadLetterJob;
import com.jobpulse.model.Job;
import com.jobpulse.model.JobHistory;
import com.jobpulse.model.Status;
import com.jobpulse.repository.DeadLetterJobRepository;
import com.jobpulse.repository.JobHistoryRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.UserRepository;
import com.jobpulse.util.CronExpressionUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    RedisTemplate<String, String> redisTemplate;
    @Autowired
    JobExecutorFactory executorFactory;

    @Autowired
    private DeadLetterJobRepository deadLetterJobRepository;
    @Autowired
    private JobHistoryRepository jobHistoryRepository;

    @Autowired
    private RetryPolicy retryPolicy;

    public JobResponse createJob(JobRequest request) {
        return mapToJobResponse(jobRepository.save(Job.builder()
                .name(request.getName())
                .cronExpression(request.getCronExpression())
                .recurring(request.isRecurring())
                .retryCount(request.getRetryCount())
                .maxRetries(request.getMaxRetries())
                .nextRunTime(request.getNextRunTime())
                .lastError(request.getLastError())
                .status(Status.PENDING)
                .build()));
    }

    public void createJobFull(JobRequestDTO dto) {
        log.info("Creating job: {}", dto.getName());
        Job job = new Job();

        job.setName(dto.getName());
        job.setJobType(dto.getJobType());
        job.setOwner(userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found with ID: " + dto.getOwnerId())));
        job.setMaxRetries(dto.getMaxRetries());
        job.setRetryCount(0);
        job.setStatus(Status.PENDING);
        job.setLastError(null);
        job.setPayload(dto.getPayload());

        ScheduleDTO scheduleDTO = dto.getSchedule();

        if (scheduleDTO.getType() == ScheduleType.ONE_TIME) {
            LocalDateTime runAt = scheduleDTO.getRunAt();
            if (runAt == null || runAt.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Invalid runAt time for ONE_TIME job. Must be in the future.");
            }
            job.setRecurring(false);
            job.setNextRunTime(runAt);
            job.setCronExpression(null);
            log.debug("Created ONE_TIME job scheduled for: {}", runAt);
            
        } else if (scheduleDTO.getType() == ScheduleType.RECURRING) {
            if (scheduleDTO.getFrequency() == null || scheduleDTO.getInterval() == null
                    || scheduleDTO.getInterval() <= 0) {
                throw new IllegalArgumentException("Invalid recurrence configuration. Frequency and interval are required and must be positive.");
            }
            job.setRecurring(true);
            String cronExpression = CronExpressionUtil.generateCronExpression(
                scheduleDTO.getFrequency().name(), 
                scheduleDTO.getInterval()
            );
            job.setCronExpression(cronExpression);
            job.setNextRunTime(CronExpressionUtil.getNextRunTime(cronExpression, LocalDateTime.now()));
            log.debug("Created RECURRING job with cron: {}", cronExpression);
            
        } else if (scheduleDTO.getType() == ScheduleType.CRON) {
            if (scheduleDTO.getCronExpression() == null || scheduleDTO.getCronExpression().isBlank()) {
                throw new IllegalArgumentException("Cron expression required for CRON schedule type.");
            }
            CronExpressionUtil.validateCronExpression(scheduleDTO.getCronExpression());
            
            job.setRecurring(true);
            job.setCronExpression(scheduleDTO.getCronExpression());
            job.setNextRunTime(CronExpressionUtil.getNextRunTime(
                scheduleDTO.getCronExpression(), 
                LocalDateTime.now()
            ));
            log.debug("Created CRON job with expression: {}", scheduleDTO.getCronExpression());
        } else {
            throw new IllegalArgumentException("Unsupported schedule type: " + scheduleDTO.getType());
        }

        jobRepository.save(job);
        log.info("Job created successfully with ID: {}", job.getId());
    }

    @Scheduled(fixedRate = 60000)
    public void runDueJobs() throws Exception {
        log.debug("Checking for due jobs...");
        List<Job> dueJobs = jobRepository.findAll()
                .stream()
                .filter(job -> job.getNextRunTime() != null &&
                        !job.getNextRunTime().isAfter(LocalDateTime.now()))
                .filter(job -> job.getStatus().equals(Status.PENDING) || job.getStatus().equals(Status.RETRYING))
                .toList();

        log.info("Found {} due jobs to execute", dueJobs.size());
        
        for (Job job : dueJobs) {
            if (acquireLock(job)) {
                try {
                    log.info("Executing job: {} (ID: {})", job.getName(), job.getId());
                    executeJob(job);
                } catch (Exception e) {
                    log.error("Error executing job {} (ID: {}): {}", job.getName(), job.getId(), e.getMessage(), e);
                } finally {
                    releaseLock(job);
                    jobRepository.save(job);
                }
            } else {
                log.debug("Could not acquire lock for job: {} (ID: {})", job.getName(), job.getId());
            }
        }
    }

    private void executeJob(Job job) throws Exception {
        try {
            job.setStatus(Status.RUNNING);
            log.info("Starting execution of job: {} (ID: {})", job.getName(), job.getId());
            
            JobExecutor executor = executorFactory.get(job.getJobType());
            if (executor == null) {
                throw new NonRetryableJobException(JobFailureReason.INVALID_CONFIG, 
                    "No executor found for job type: " + job.getJobType());
            }
            
            executor.execute(job);

            job.setStatus(Status.SUCCESS);
            job.setRetryCount(0);
            job.setLastError(null);

            jobHistoryRepository.save(
                    JobHistory.builder()
                            .job(job)
                            .status(Status.SUCCESS)
                            .retryAttempt(0)
                            .runTime(LocalDateTime.now())
                            .build());

            job.setNextRunTime(calculateNextRun(job));
            log.info("Job executed successfully: {} (ID: {})", job.getName(), job.getId());

        } catch (JobExecutionException e) {
            log.error("Job execution failed: {} - Reason: {}, Message: {}", job.getName(), e.getReason(), e.getMessage());

            JobFailureReason reason = e.getReason();
            boolean retryable = retryPolicy.isRetryable(reason);

            int nextRetry = job.getRetryCount() + 1;

            if (retryable && nextRetry <= job.getMaxRetries()) {
                long delayMinutes = (long) Math.pow(2, nextRetry);
                job.setRetryCount(nextRetry);
                job.setStatus(Status.RETRYING);
                job.setNextRunTime(LocalDateTime.now().plusMinutes(delayMinutes));
                
                log.info("Job scheduled for retry {} of {} with {} minute delay: {}", 
                    nextRetry, job.getMaxRetries(), delayMinutes, job.getName());

            } else {
                job.setStatus(Status.FAILED);
                log.warn("Job failed and will not be retried: {} (Retryable: {}, Attempt: {} of {})", 
                    job.getName(), retryable, nextRetry, job.getMaxRetries());
                moveToDeadLetter(job);
            }

            job.setLastError(e.getMessage());

            jobHistoryRepository.save(
                    JobHistory.builder()
                            .job(job)
                            .status(job.getStatus())
                            .retryAttempt(nextRetry)
                            .errorMessage(e.getMessage())
                            .runTime(LocalDateTime.now())
                            .build());
        }
    }

    private boolean acquireLock(Job job) {
        String key = "job-lock:" + job.getId();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", 5, TimeUnit.MINUTES);
        return locked != null && locked;
    }

    private void releaseLock(Job job) {
        redisTemplate.delete("job-lock:" + job.getId());
    }

    private void moveToDeadLetter(Job job) {
        deadLetterJobRepository.save(
                DeadLetterJob.builder()
                        .job(job)
                        .lastError(job.getLastError())
                        .failedAt(LocalDateTime.now())
                        .build());
        jobRepository.delete(job);
    }

    private LocalDateTime calculateNextRun(Job job) {
        if (!job.isRecurring() || job.getCronExpression() == null) {
            return null;
        }
        try {
            return CronExpressionUtil.getNextRunTime(job.getCronExpression(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error calculating next run time for job {}: {}", job.getId(), e.getMessage());
            return LocalDateTime.now().plusMinutes(5);
        }
    }

    public List<JobResponse> getJobs() {
        return jobRepository.findAll().stream().map(JobService::mapToJobResponse).toList();
    }

    public JobResponse getJob(long id) {
        return mapToJobResponse(
                jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found")));
    }

    public void deleteJob(long id) {
        jobRepository.deleteById(id);
    }

    public JobResponse replayDeadJob(Long deadJobId) {
        log.info("Replaying dead letter job: {}", deadJobId);
        
        DeadLetterJob dlJob = deadLetterJobRepository.findById(deadJobId)
                .orElseThrow(() -> new ResourceNotFoundException("Dead letter job not found with ID: " + deadJobId));

        Job originalJob = dlJob.getJob();

        originalJob.setRetryCount(0);
        originalJob.setStatus(Status.PENDING);
        originalJob.setLastError(null);

        if (originalJob.isRecurring() && originalJob.getCronExpression() != null) {
            try {
                originalJob.setNextRunTime(CronExpressionUtil.getNextRunTime(
                    originalJob.getCronExpression(), 
                    LocalDateTime.now()
                ));
            } catch (Exception e) {
                log.warn("Failed to parse cron expression, scheduling for immediate execution");
                originalJob.setNextRunTime(LocalDateTime.now().plusSeconds(1));
            }
        } else {
            originalJob.setNextRunTime(LocalDateTime.now().plusSeconds(1));
        }

        Job savedJob = jobRepository.save(originalJob);
        deadLetterJobRepository.delete(dlJob);
        
        log.info("Dead letter job replayed successfully: {} (ID: {})", originalJob.getName(), savedJob.getId());
        return mapToJobResponse(savedJob);
    }

    public List<DeadLetterJobResponse> getDeadLetterJobs() {
        return deadLetterJobRepository.findAll().stream()
                .map(dlj -> DeadLetterJobResponse.builder()
                        .id(dlj.getId())
                        .jobName(dlj.getJob().getName())
                        .jobType(dlj.getJob().getJobType() != null ? dlj.getJob().getJobType().name() : null)
                        .lastError(dlj.getLastError())
                        .failedAt(dlj.getFailedAt())
                        .retryCount(dlj.getJob().getRetryCount())
                        .maxRetries(dlj.getJob().getMaxRetries())
                        .build())
                .toList();
    }

    public List<JobHistoryResponse> getJobHistory(long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return jobHistoryRepository.findByJobOrderByRunTimeDesc(job).stream()
                .map(jh -> JobHistoryResponse.builder()
                        .id(jh.getId())
                        .runTime(jh.getRunTime())
                        .status(jh.getStatus())
                        .errorMessage(jh.getErrorMessage())
                        .retryAttempt(jh.getRetryAttempt())
                        .build())
                .toList();
    }

    public JobStatsResponse getJobStats() {
        List<Job> allJobs = jobRepository.findAll();
        List<DeadLetterJob> deadLetterJobs = deadLetterJobRepository.findAll();

        long pending = allJobs.stream().filter(j -> j.getStatus() == Status.PENDING).count();
        long running = allJobs.stream().filter(j -> j.getStatus() == Status.RUNNING).count();
        long success = allJobs.stream().filter(j -> j.getStatus() == Status.SUCCESS).count();
        long retrying = allJobs.stream().filter(j -> j.getStatus() == Status.RETRYING).count();
        long failed = allJobs.stream().filter(j -> j.getStatus() == Status.FAILED).count();

        return JobStatsResponse.builder()
                .totalJobs(allJobs.size())
                .pendingJobs(pending)
                .runningJobs(running)
                .successfulJobs(success)
                .retryingJobs(retrying)
                .failedJobs(failed)
                .deadLetterJobs(deadLetterJobs.size())
                .build();
    }

    public JobResponse pauseJob(long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setStatus(Status.PAUSED);
        return mapToJobResponse(jobRepository.save(job));
    }

    public JobResponse resumeJob(long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setStatus(Status.PENDING);
        if (job.getNextRunTime() == null || job.getNextRunTime().isBefore(LocalDateTime.now())) {
            job.setNextRunTime(LocalDateTime.now().plusSeconds(10));
        }
        return mapToJobResponse(jobRepository.save(job));
    }

    public static JobResponse mapToJobResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .cronExpression(job.getCronExpression())
                .maxRetries(job.getMaxRetries())
                .status(job.getStatus())
                .lastError(job.getLastError())
                .recurring(job.isRecurring())
                .retryCount(job.getRetryCount())
                .nextRunTime(job.getNextRunTime())
                .jobType(job.getJobType() != null ? job.getJobType().name() : null)
                .payload(job.getPayload())
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class DeadLetterJobResponse {
        private Long id;
        private String jobName;
        private String jobType;
        private String lastError;
        private LocalDateTime failedAt;
        private int retryCount;
        private int maxRetries;
    }

    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class JobHistoryResponse {
        private Long id;
        private LocalDateTime runTime;
        private Status status;
        private String errorMessage;
        private int retryAttempt;
    }

    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class JobStatsResponse {
        private long totalJobs;
        private long pendingJobs;
        private long runningJobs;
        private long successfulJobs;
        private long retryingJobs;
        private long failedJobs;
        private long deadLetterJobs;
    }
}
