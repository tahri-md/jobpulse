package com.jobpulse.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.jobpulse.dto.request.JobRequest;
import com.jobpulse.dto.request.JobRequestDTO;
import com.jobpulse.dto.request.JobRequestDTO.ScheduleDTO;
import com.jobpulse.dto.response.JobResponse;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.model.DeadLetterJob;
import com.jobpulse.model.Job;
import com.jobpulse.model.JobHistory;
import com.jobpulse.model.Status;
import com.jobpulse.repository.DeadLetterJobRepository;
import com.jobpulse.repository.JobHistoryRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.UserRepository;

@Service
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
        Job job = new Job();

        job.setName(dto.getName());
        job.setOwner(userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found")));
        job.setMaxRetries(dto.getMaxRetries());
        job.setRetryCount(0);
        job.setStatus(Status.SCHEDULED);
        job.setLastError(null);
        job.setPayload(dto.getPayload());

        JobRequestDTO.ScheduleDTO scheduleDTO = dto.getSchedule();

        if (scheduleDTO.getType() == JobRequestDTO.ScheduleType.ONE_TIME) {
            LocalDateTime runAt = scheduleDTO.getRunAt();
            if (runAt == null || runAt.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Invalid runAt time for ONE_TIME job");
            }
            job.setRecurring(false);
            job.setNextRunTime(runAt);
            job.setCronExpression(null);
        } else if (scheduleDTO.getType() == JobRequestDTO.ScheduleType.RECURRING) {
            if (scheduleDTO.getFrequency() == null || scheduleDTO.getInterval() == null
                    || scheduleDTO.getInterval() <= 0) {
                throw new RuntimeException("Invalid recurrence configuration");
            }
            job.setRecurring(true);
            switch (scheduleDTO.getFrequency()) {
                case MINUTES:
                    job.setCronExpression("0 */" + scheduleDTO.getInterval() + " * * * *");
                    break;
                case HOURS:
                    job.setCronExpression("0 0 */" + scheduleDTO.getInterval() + " * * *");
                    break;
                case DAYS:
                    job.setCronExpression("0 0 0 */" + scheduleDTO.getInterval() + " * *");
                    break;
                default:
                    throw new RuntimeException("Unsupported frequency type");
            }
            job.setNextRunTime(CronExpression.parse(job.getCronExpression()).next(LocalDateTime.now()));
        } else if (scheduleDTO.getType() == JobRequestDTO.ScheduleType.CRON) {
            if (scheduleDTO.getCronExpression() == null || scheduleDTO.getCronExpression().isBlank()) {
                throw new RuntimeException("Cron expression required for CRON job");
            }
            try {
                CronExpression.parse(scheduleDTO.getCronExpression());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid cron expression: " + e.getMessage());
            }
            job.setRecurring(true);
            job.setCronExpression(scheduleDTO.getCronExpression());
            job.setNextRunTime(CronExpression.parse(job.getCronExpression()).next(LocalDateTime.now()));
        } else {
            throw new RuntimeException("Unsupported schedule type");
        }

        jobRepository.save(job);
    }

    @Scheduled(fixedRate = 60000)
    public void runDueJobs() {
        List<Job> dueJobs = jobRepository.findAll()
                .stream()
                .filter(job -> job.getNextRunTime() != null &&
                        !job.getNextRunTime().isAfter(LocalDateTime.now()))
                .filter(job -> job.getStatus().equals(Status.SCHEDULED))
                .toList();

        for (Job job : dueJobs) {
            if (acquireLock(job)) {
                try {
                    executeJob(job);
                } finally {
                    releaseLock(job);
                    jobRepository.save(job);
                }
            }
        }
    }

    private void executeJob(Job job) {
        try {
            JobExecutor executor = executorFactory.get(job.getJobType());
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

        } catch (Exception e) {

            int nextRetry = job.getRetryCount() + 1;
            job.setRetryCount(nextRetry);
            job.setStatus(Status.FAILED);
            job.setLastError(e.getMessage());

            jobHistoryRepository.save(
                    JobHistory.builder()
                            .job(job)
                            .status(Status.FAILED)
                            .retryAttempt(nextRetry)
                            .errorMessage(e.getMessage())
                            .runTime(LocalDateTime.now())
                            .build());

            if (nextRetry > job.getMaxRetries()) {
                moveToDeadLetter(job);
            } else {
                long delayMinutes = (long) Math.pow(2, nextRetry);
                job.setNextRunTime(LocalDateTime.now().plusMinutes(delayMinutes));
            }
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
        if (!job.isRecurring()) {
            return null;
        }
        return LocalDateTime.now().plusMinutes(5);
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

    public static JobResponse mapToJobResponse(Job job) {
        return JobResponse.builder()
                .name(job.getName())
                .cronExpression(job.getCronExpression())
                .maxRetries(job.getMaxRetries())
                .status(job.getStatus())
                .lastError(job.getLastError())
                .recurring(job.isRecurring())
                .retryCount(job.getRetryCount())
                .nextRunTime(job.getNextRunTime())
                .status(job.getStatus())
                .build();
    }
}
