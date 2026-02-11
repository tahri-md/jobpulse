import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JobService } from '../../../services/job.service';
import { ToastService } from '../../../services/toast.service';
import { FullJobRequest, JobType, ScheduleType, TimeUnit } from '../../../models';

@Component({
  selector: 'app-job-create',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button class="back-btn" (click)="goBack()">&larr; Back to Jobs</button>
          <h1>Create New Job</h1>
          <p class="subtitle">Configure and schedule a new background job</p>
        </div>
      </div>

      <form (ngSubmit)="onSubmit()" class="form-container">
        <div class="form-section">
          <h2>Job Details</h2>

          <div class="form-group">
            <label for="name">Job Name *</label>
            <input id="name" type="text" [(ngModel)]="job.name" name="name" placeholder="e.g., Daily Report Generator" required />
          </div>

          <div class="form-group">
            <label for="type">Job Type *</label>
            <select id="type" [(ngModel)]="job.jobType" name="type" (ngModelChange)="onTypeChange($event)" required>
              @for (t of jobTypes; track t) {
                <option [value]="t">{{ formatType(t) }}</option>
              }
            </select>
            <span class="form-hint">{{ getTypeDescription(job.jobType) }}</span>
          </div>

          <div class="form-group">
            <label for="payload">
              Payload
              <button type="button" class="reset-btn" (click)="resetPayload()">Reset template</button>
            </label>
            <textarea id="payload" [(ngModel)]="job.payload" name="payload" placeholder='{"key": "value"}' [rows]="payloadRows"></textarea>
            <span class="form-hint">{{ getPayloadHint(job.jobType) }}</span>
          </div>

          <div class="form-group">
            <label for="maxRetries">Max Retries</label>
            <input id="maxRetries" type="number" [(ngModel)]="job.maxRetries" name="maxRetries" min="0" max="10" />
          </div>
        </div>

        <div class="form-section">
          <h2>Schedule</h2>

          <div class="form-group">
            <label for="scheduleType">Schedule Type *</label>
            <select id="scheduleType" [(ngModel)]="job.schedule.type" name="scheduleType" required>
              @for (st of scheduleTypes; track st) {
                <option [value]="st">{{ formatScheduleType(st) }}</option>
              }
            </select>
          </div>

          @if (job.schedule.type === 'ONE_TIME') {
            <div class="form-group">
              <label for="executeAt">Execute At *</label>
              <input id="executeAt" type="datetime-local" [(ngModel)]="executeAtStr" name="executeAt" required />
            </div>
          }

          @if (job.schedule.type === 'RECURRING') {
            <div class="form-row">
              <div class="form-group">
                <label for="interval">Interval *</label>
                <input id="interval" type="number" [(ngModel)]="job.schedule.interval" name="interval" min="1" required />
              </div>
              <div class="form-group">
                <label for="frequency">Frequency *</label>
                <select id="frequency" [(ngModel)]="job.schedule.frequency" name="frequency" required>
                  @for (tu of timeUnits; track tu) {
                    <option [value]="tu">{{ tu }}</option>
                  }
                </select>
              </div>
            </div>
          }

          @if (job.schedule.type === 'CRON') {
            <div class="form-group">
              <label for="cronExpression">Cron Expression *</label>
              <input id="cronExpression" type="text" [(ngModel)]="job.schedule.cronExpression" name="cronExpression" placeholder="0 0 * * * (every hour)" required />
              <span class="form-hint">Standard cron format: minute hour day month weekday</span>
            </div>
          }
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" (click)="goBack()">Cancel</button>
          <button type="submit" class="btn btn-primary" [disabled]="submitting">
            {{ submitting ? 'Creating...' : 'Create Job' }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .page { max-width: 720px; }
    .page-header { margin-bottom: 28px; }
    .page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); margin: 4px 0 6px; letter-spacing: -0.5px; }
    .subtitle { font-size: 14px; color: var(--text-muted); margin: 0; }
    .back-btn {
      background: none; border: none; cursor: pointer; color: var(--accent);
      font-size: 13px; font-weight: 500; padding: 0;
    }
    .back-btn:hover { text-decoration: underline; }
    .form-container { display: flex; flex-direction: column; gap: 20px; }
    .form-section {
      background: var(--bg-surface); border-radius: 8px; padding: 24px; border: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
    }
    .form-section h2 {
      font-size: 14px; font-weight: 600; color: var(--text-primary); margin: 0 0 20px;
      padding-bottom: 12px; border-bottom: 1px solid var(--border);
    }
    .form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 18px; }
    .form-group:last-child { margin-bottom: 0; }
    .form-group label {
      font-size: 13px; font-weight: 600; color: var(--text-secondary); display: flex;
      align-items: center; justify-content: space-between;
    }
    .form-group input, .form-group select, .form-group textarea {
      padding: 10px 14px; border: 1px solid var(--border); border-radius: 6px; font-size: 14px;
      outline: none; transition: border-color 0.15s; background: var(--bg-input); color: var(--text-input); font-family: inherit;
    }
    .form-group input:focus, .form-group select:focus, .form-group textarea:focus {
      border-color: var(--accent);
    }
    .form-group input::placeholder, .form-group textarea::placeholder { color: var(--text-placeholder); }
    .form-group textarea { font-family: 'JetBrains Mono', monospace; font-size: 13px; resize: vertical; }
    .form-hint { font-size: 12px; color: var(--text-muted); }
    .reset-btn {
      background: none; border: 1px solid var(--border-hover); border-radius: 4px; padding: 2px 10px;
      font-size: 11px; color: var(--text-muted); cursor: pointer; font-weight: 500; transition: all 0.15s;
    }
    .reset-btn:hover { border-color: var(--text-muted); color: var(--text-secondary); }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 12px; }
    .btn {
      padding: 10px 24px; border: none; border-radius: 6px; font-size: 13px; font-weight: 600;
      cursor: pointer; transition: all 0.15s;
    }
    .btn-primary { background: var(--accent); color: var(--accent-text); }
    .btn-primary:hover:not(:disabled) { background: var(--accent-hover); }
    .btn-secondary { background: var(--bg-elevated); color: var(--text-muted); border: 1px solid var(--border-hover); }
    .btn-secondary:hover { background: var(--bg-hover); color: var(--text-secondary); }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }
  `]
})
export class JobCreateComponent {
  jobTypes = Object.values(JobType);
  scheduleTypes = Object.values(ScheduleType);
  timeUnits = Object.values(TimeUnit);
  executeAtStr = '';
  submitting = false;
  payloadRows = 6;

  private readonly payloadTemplates: Record<string, object> = {
    [JobType.LOG]: {
      message: 'Hello from JobPulse!',
      level: 'INFO'
    },
    [JobType.EMAIL]: {
      to: 'recipient@example.com',
      subject: 'Email Subject',
      body: 'Email body text',
      from: 'sender@example.com',
      cc: ''
    },
    [JobType.HTTP_CALL]: {
      url: 'https://api.example.com/endpoint',
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      body: null,
      timeoutSeconds: 30
    },
    [JobType.SCRIPT]: {
      command: 'echo "Hello from JobPulse"',
      timeoutSeconds: 300
    },
    [JobType.DATA_CLEANUP]: {
      action: 'delete_old_records',
      tableName: 'logs',
      dateColumn: 'created_at',
      daysOld: 30
    },
    [JobType.REPORT_GENERATION]: {
      reportType: 'job_execution_stats',
      outputFormat: 'CSV'
    }
  };

  private readonly typeDescriptions: Record<string, string> = {
    [JobType.LOG]:               'Logs a message at the specified level (DEBUG, INFO, WARN, ERROR)',
    [JobType.EMAIL]:             'Sends an email via the configured mail server',
    [JobType.HTTP_CALL]:              'Makes an HTTP request to an external API',
    [JobType.SCRIPT]:            'Executes a shell command on the server',
    [JobType.DATA_CLEANUP]:      'Runs database cleanup: delete old records, truncate, or archive',
    [JobType.REPORT_GENERATION]: 'Generates a report: user_activity, job_execution_stats, or system_health'
  };

  private readonly payloadHints: Record<string, string> = {
    [JobType.LOG]:               'Required: message  •  Optional: level',
    [JobType.EMAIL]:             'Required: to, subject, body  •  Optional: from, cc',
    [JobType.HTTP_CALL]:              'Required: url, method  •  Optional: headers, body, timeoutSeconds',
    [JobType.SCRIPT]:            'Required: command  •  Optional: timeoutSeconds',
    [JobType.DATA_CLEANUP]:      'Required: action (delete_old_records | truncate_table | archive_data) + action-specific fields',
    [JobType.REPORT_GENERATION]: 'Required: reportType (user_activity | job_execution_stats | system_health)  •  Optional: outputFormat'
  };

  job: FullJobRequest = {
    name: '',
    jobType: JobType.LOG,
    payload: this.prettify(JobType.LOG),
    maxRetries: 3,
    schedule: {
      type: ScheduleType.ONE_TIME,
      interval: 1,
      frequency: TimeUnit.MINUTES
    }
  };

  constructor(
    private jobService: JobService,
    private router: Router,
    private toast: ToastService
  ) {}

  onSubmit(): void {
    if (!this.job.name) {
      this.toast.warning('Please provide a job name.');
      return;
    }

    if (this.job.schedule.type === ScheduleType.ONE_TIME) {
      if (!this.executeAtStr) {
        this.toast.warning('Please specify when to execute the job.');
        return;
      }
      // Send as LocalDateTime format (yyyy-MM-ddTHH:mm:ss) without timezone
      this.job.schedule.runAt = this.executeAtStr + ':00';
    }

    this.submitting = true;
    this.jobService.createFull(this.job).subscribe({
      next: () => {
        this.toast.success('Job created successfully!');
        this.router.navigate(['/jobs']);
      },
      error: (err: any) => {
        this.submitting = false;
        const msg = err.error?.message || 'Failed to create job.';
        this.toast.error(msg);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/jobs']);
  }

  formatType(t: string): string {
    return t.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  formatScheduleType(t: string): string {
    return t.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  onTypeChange(type: JobType): void {
    this.job.payload = this.prettify(type);
    this.payloadRows = this.job.payload.split('\n').length + 1;
  }

  resetPayload(): void {
    this.job.payload = this.prettify(this.job.jobType);
    this.payloadRows = this.job.payload.split('\n').length + 1;
  }
  getTypeDescription(type: JobType): string {
    return this.typeDescriptions[type] || '';
  }

  getPayloadHint(type: JobType): string {
    return this.payloadHints[type] || 'JSON payload for the job executor';
  }

  private prettify(type: JobType): string {
    const template = this.payloadTemplates[type];
    return template ? JSON.stringify(template, null, 2) : '{}';
  }
}
