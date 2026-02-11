import { JobType, ScheduleType, Status, TimeUnit, JobFailureReason } from './enums';

export interface HttpJobPayload {
  url: string;
  method: string;
  headers: Record<string, string>;
  body: any;
  timeoutSeconds: number;
}

export interface ScheduleRequest {
  type: ScheduleType;
  runAt?: string;
  frequency?: TimeUnit;
  interval?: number;
  cronExpression?: string;
}

export interface JobRequest {
  name: string;
  type: string;
  payload: string;
  retryable: boolean;
  maxRetries: number;
  retryCount: number;
  httpPayload?: HttpJobPayload;
  cronExpression?: string;
}

export interface FullJobRequest {
  name: string;
  ownerId?: number;
  jobType: JobType;
  payload: string;
  schedule: ScheduleRequest;
  maxRetries: number;
}

export interface JobResponse {
  id: number;
  name: string;
  jobType: string;
  payload: string;
  cronExpression: string;
  recurring: boolean;
  status: Status;
  maxRetries: number;
  retryCount: number;
  nextRunTime: string;
  lastError: string;
}

export interface JobHistoryResponse {
  id: number;
  runTime: string;
  status: string;
  errorMessage: string;
  retryAttempt: number;
}

export interface DeadLetterJobResponse {
  id: number;
  jobName: string;
  jobType: string;
  lastError: string;
  failedAt: string;
  retryCount: number;
  maxRetries: number;
}

export interface JobStatsResponse {
  totalJobs: number;
  pendingJobs: number;
  runningJobs: number;
  successfulJobs: number;
  retryingJobs: number;
  failedJobs: number;
  deadLetterJobs: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details: any;
  path: string;
}
