# JobPulse

A full-stack job scheduling and monitoring platform built with Spring Boot and Angular. JobPulse allows users to create, schedule, and monitor background jobs with automatic retries, dead letter queues, and real-time status tracking.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features](#features)
- [Job Types](#job-types)
- [Scheduling](#scheduling)
- [Retry and Dead Letter Queue](#retry-and-dead-letter-queue)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Infrastructure](#infrastructure)

---

## Overview

JobPulse is a platform where users can define and schedule background tasks (HTTP calls, emails, scripts, log entries, data cleanup, and report generation). The system handles execution, retries with exponential backoff, and moves permanently failed jobs to a dead letter queue for manual review and replay.

---

## Architecture

```
                          +-------------------+
                          |     Frontend      |
                          |   Angular / Nginx |
                          |    (port 4200)    |
                          +---------+---------+
                                    |
                              /api/* proxy
                                    |
                          +---------+---------+
                          |      Backend      |
                          |   Spring Boot     |
                          |    (port 8080)    |
                          +---------+---------+
                           /                 \
                  +-------+-------+    +------+------+
                  |   PostgreSQL  |    |    Redis     |
                  |  (port 5432)  |    | (port 6379)  |
                  +---------------+    +--------------+
```

**Request flow:**

1. The browser loads the Angular SPA from nginx.
2. All `/api/*` requests are reverse-proxied by nginx to the Spring Boot backend.
3. The backend reads and writes data to PostgreSQL.
4. Redis is used for caching and distributed locks during job execution.

---

## Tech Stack

### Backend

| Component        | Version |
|------------------|---------|
| Java             | 21      |
| Spring Boot      | 4.0.2   |
| Spring Security  | --      |
| Spring Data JPA  | --      |
| Spring Data Redis| --      |
| Spring Quartz    | --      |
| Resilience4j     | --      |
| Flyway           | --      |
| PostgreSQL       | 16      |
| Redis            | 7       |
| JJWT             | 0.12.6  |

### Frontend

| Component   | Version |
|-------------|---------|
| Angular     | 21.1    |
| TypeScript  | 5.9     |
| Node.js     | 22      |
| Nginx       | Alpine  |

---

## Project Structure

```
jobpulse-project/
    docker-compose.yml          # Multi-service orchestration
    .env                        # Environment variables (git-ignored)
    .env.example                # Template for .env

    jobpulse/                   # Backend (Spring Boot)
        src/main/java/com/jobpulse/
            model/              # JPA entities (User, Job, JobHistory, DeadLetterJob, GmailToken)
            repository/         # Spring Data JPA repositories
            service/            # Business logic, job executors, scheduling
            controller/         # REST controllers
            config/             # Security, JWT, Redis, Jackson configuration
            dto/                # Request/response data transfer objects
            exception/          # Custom exception classes
            annotation/         # Custom annotations (ExecutorType)
            util/               # Cron expression utilities
        src/main/resources/
            application.properties
            db/migration/       # Flyway migration scripts

    jobpulse-ui/                # Frontend (Angular)
        src/app/
            pages/              # Route-level components (login, register, dashboard, jobs, etc.)
            components/         # Shared components (layout, sidebar, toast)
            services/           # HTTP services (auth, job, oauth, gmail)
            guards/             # Route guards (auth)
            interceptors/       # HTTP interceptors (JWT token injection)
            models/             # TypeScript interfaces and enums
```

---

## Features

### Job Lifecycle

- Create jobs with a name, type, payload, and schedule.
- Jobs transition through statuses: PENDING, RUNNING, SUCCESS, RETRYING, FAILED, PAUSED.
- Pause and resume jobs at any time.
- View execution history per job.
- Dashboard with job statistics.

### Distributed Execution

- A scheduler runs every 60 seconds to pick up due jobs.
- Redis distributed locks (with a 5-minute TTL) prevent the same job from being executed concurrently across multiple instances.
- Maximum concurrent job limit is configurable (default: 10).

### Job Executor Pattern

Each job type is handled by a dedicated executor class, resolved at runtime through a factory pattern using a custom `@ExecutorType` annotation:

| Executor                     | Annotation Value |
|------------------------------|-----------------|
| HttpJobExecutor              | HTTP            |
| EmailJobExecutor             | EMAIL           |
| ScriptJobExecutor            | SCRIPT          |
| LogJobExecutor               | LOG             |
| DataCleanupJobExecutor       | DATA_CLEANUP    |
| ReportGenerationJobExecutor  | REPORT          |

---

## Job Types

| Type         | Description                                      |
|--------------|--------------------------------------------------|
| HTTP         | Makes HTTP requests to external endpoints        |
| EMAIL        | Sends emails via Gmail OAuth or SMTP             |
| SCRIPT       | Executes scripts                                 |
| LOG          | Writes log entries                               |
| DATA_CLEANUP | Cleans up stale or expired data                  |
| REPORT       | Generates reports                                |

---

## Scheduling

Jobs can be scheduled in three ways:

| Schedule Type | Description                                           | Required Fields            |
|---------------|-------------------------------------------------------|----------------------------|
| ONE_TIME      | Runs once at a specified time                         | `scheduledAt` (future)     |
| RECURRING     | Runs at a fixed interval                              | `intervalUnit` + `intervalValue` |
| CRON          | Runs on a cron expression                             | `cronExpression`           |

For recurring jobs, the system auto-generates a cron expression from the interval unit (MINUTES, HOURS, DAYS) and value.

---

## Retry and Dead Letter Queue

When a job fails, the system evaluates whether the failure is retryable:

**Retryable failures** (will be retried with exponential backoff):
- Connection timeouts
- HTTP 5xx responses
- Rate limiting (429)
- Temporary network errors

**Non-retryable failures** (move directly to dead letter queue):
- Authentication errors
- HTTP 4xx responses (except 429)
- Validation errors
- Configuration errors

Retry delay follows exponential backoff: delay = 2^n minutes, where n is the current retry attempt.

When a job exhausts all retry attempts (configurable `maxRetries` per job), it is moved to the `dead_letter_jobs` table. Dead letter jobs can be reviewed and replayed from the UI.

---

## Authentication

### JWT

- Access tokens expire after 1 hour.
- Refresh tokens expire after 7 days.
- Passwords are hashed with BCrypt.
- The `JwtAuthFilter` extracts and validates the token on every request (except public endpoints).

### OAuth

Users can sign in with Google or GitHub. The OAuth flow exchanges an authorization code for user info, then either creates a new account or links to an existing one.

### Gmail Integration

For email-type jobs, users can connect their Gmail account via OAuth. Gmail tokens are encrypted with AES before being stored in the database.

### Public Endpoints

The following paths do not require authentication:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/oauth/**`
- `GET /actuator/health`

---

## API Endpoints

### Auth

| Method | Path                        | Description           |
|--------|-----------------------------|-----------------------|
| POST   | `/api/v1/auth/register`     | Register a new user   |
| POST   | `/api/v1/auth/login`        | Login and get tokens  |
| GET    | `/api/v1/auth/me`           | Get current user info |

### OAuth

| Method | Path                        | Description                 |
|--------|-----------------------------|-----------------------------|
| POST   | `/api/v1/auth/oauth/google` | Google OAuth login          |
| POST   | `/api/v1/auth/oauth/github` | GitHub OAuth login          |

### Jobs

| Method | Path                                  | Description                 |
|--------|---------------------------------------|-----------------------------|
| POST   | `/api/v1/jobs/full`                   | Create a new job            |
| GET    | `/api/v1/jobs`                        | List all user jobs          |
| GET    | `/api/v1/jobs/{id}`                   | Get job details             |
| DELETE | `/api/v1/jobs/{id}`                   | Delete a job                |
| PUT    | `/api/v1/jobs/{id}/pause`             | Pause a job                 |
| PUT    | `/api/v1/jobs/{id}/resume`            | Resume a job                |
| GET    | `/api/v1/jobs/stats`                  | Get job statistics          |
| GET    | `/api/v1/jobs/{id}/history`           | Get job execution history   |
| GET    | `/api/v1/jobs/dead-letter`            | List dead letter jobs       |
| POST   | `/api/v1/jobs/dead-letter/{id}/replay`| Replay a dead letter job    |

### Gmail

| Method | Path                        | Description                      |
|--------|-----------------------------|---------------------------------|
| GET    | `/api/v1/gmail/auth-url`    | Get Gmail OAuth consent URL      |
| POST   | `/api/v1/gmail/callback`    | Handle OAuth callback            |
| GET    | `/api/v1/gmail/status`      | Check Gmail connection status    |
| DELETE | `/api/v1/gmail/disconnect`  | Disconnect Gmail account         |

---

## Getting Started

### Prerequisites

- Docker and Docker Compose

### Setup

1. Clone the repository:

```bash
git clone https://github.com/tahri-md/jobpulse.git
cd jobpulse
```

2. Create an `.env` file from the template:

```bash
cp .env.example .env
```

3. Fill in your secrets in `.env`:

```
JWT_SECRET=your-jwt-secret-min-32-chars-long
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

4. Start all services:

```bash
docker-compose up --build -d
```

5. Access the application:

| Service   | URL                       |
|-----------|---------------------------|
| Frontend  | http://localhost:4200      |
| Backend   | http://localhost:8080      |
| API docs  | http://localhost:8080/actuator/health |

---

## Configuration

All configuration is driven by environment variables, with defaults defined in `application.properties`.

### Database

| Variable      | Default     | Description          |
|---------------|-------------|----------------------|
| `DB_HOST`     | `localhost` | PostgreSQL host      |
| `DB_PORT`     | `5432`      | PostgreSQL port      |
| `DB_NAME`     | `jobpulse`  | Database name        |
| `DB_USERNAME` | `amine`     | Database user        |
| `DB_PASSWORD` | `amine`     | Database password    |
| `DB_POOL_SIZE`| `20`        | Max connection pool  |

### Redis

| Variable        | Default     | Description     |
|-----------------|-------------|-----------------|
| `REDIS_HOST`    | `localhost` | Redis host      |
| `REDIS_PORT`    | `6379`      | Redis port      |
| `REDIS_PASSWORD`| (empty)     | Redis password  |

### JWT

| Variable        | Default                | Description             |
|-----------------|------------------------|-------------------------|
| `JWT_SECRET`    | (generated default)    | Signing key             |
| `JWT_EXPIRATION`| `3600000` (1 hour)     | Access token TTL (ms)   |

### Job Scheduler

| Variable              | Default | Description                    |
|-----------------------|---------|--------------------------------|
| `SCHEDULER_POOL_SIZE` | `10`    | Scheduler thread pool size     |
| `EXECUTOR_CORE_SIZE`  | `10`    | Executor core thread pool size |
| `EXECUTOR_MAX_SIZE`   | `20`    | Executor max thread pool size  |

---

## Infrastructure

### Docker Services

| Container     | Image                   | Port  | Purpose                    |
|---------------|-------------------------|-------|----------------------------|
| `postgres-db` | `postgres:16`           | 5432  | Relational data storage    |
| `redis-cache` | `redis:7`               | 6379  | Caching, distributed locks |
| `jobpulse-api`| Built from `jobpulse/`  | 8080  | Spring Boot REST API       |
| `jobpulse-ui` | Built from `jobpulse-ui/`| 4200 | Angular SPA via Nginx      |

### Build Process

**Backend** (multi-stage):
- Stage 1: `eclipse-temurin:21-jdk` -- Maven build, dependency caching, package as JAR.
- Stage 2: `eclipse-temurin:21-jre` -- Copies the JAR, runs with `java -jar`.

**Frontend** (multi-stage):
- Stage 1: `node:22-alpine` -- `npm ci`, `ng build --configuration production`.
- Stage 2: `nginx:alpine` -- Serves the compiled static files, proxies `/api` to backend.

### Health Checks

- PostgreSQL: `pg_isready` every 5 seconds.
- Redis: `redis-cli ping` every 5 seconds.
- Backend: `curl /actuator/health` every 10 seconds (30s start period).
- Frontend starts only after backend is healthy.

### Data Persistence

- PostgreSQL data is persisted in the `pgdata` Docker volume.
- Redis data is persisted in the `redisdata` Docker volume with AOF (append-only file) enabled.
