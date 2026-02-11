import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobService } from '../../../services/job.service';
import { ToastService } from '../../../services/toast.service';
import { JobResponse, JobHistoryResponse } from '../../../models';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page">
      @if (loading) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Loading...</p>
        </div>
      } @else if (job) {
        <div class="page-header">
          <div>
            <button class="back-btn" (click)="goBack()">&larr; Back to Jobs</button>
            <h1>{{ job.name }}</h1>
            <span class="badge" [class]="'badge badge-' + (job.status || '').toLowerCase()">{{ job.status }}</span>
          </div>
          <div class="header-actions">
            @if (job.status === 'PENDING' || job.status === 'RUNNING' || job.status === 'RETRYING') {
              <button class="btn btn-outline" (click)="pauseJob()">Pause</button>
            }
            @if (job.status === 'PAUSED') {
              <button class="btn btn-primary" (click)="resumeJob()">Resume</button>
            }
            <button class="btn btn-danger" (click)="deleteJob()">Delete</button>
          </div>
        </div>

        <div class="info-grid">
          <div class="info-card">
            <h3>General</h3>
            <div class="info-row"><span class="info-label">ID</span><span class="info-value mono">#{{ job.id }}</span></div>
            <div class="info-row"><span class="info-label">Name</span><span class="info-value">{{ job.name }}</span></div>
            <div class="info-row"><span class="info-label">Type</span><span class="info-value"><span class="badge badge-type">{{ job.jobType }}</span></span></div>
            <div class="info-row"><span class="info-label">Status</span><span class="info-value"><span class="badge" [class]="'badge badge-' + (job.status || '').toLowerCase()">{{ job.status }}</span></span></div>
          </div>

          <div class="info-card">
            <h3>Retry Configuration</h3>
            <div class="info-row"><span class="info-label">Recurring</span><span class="info-value">{{ job.recurring ? 'Yes' : 'No' }}</span></div>
            <div class="info-row"><span class="info-label">Max Retries</span><span class="info-value mono">{{ job.maxRetries }}</span></div>
            <div class="info-row"><span class="info-label">Retry Count</span><span class="info-value mono">{{ job.retryCount }}</span></div>
            <div class="info-row"><span class="info-label">Next Run</span><span class="info-value">{{ job.nextRunTime ? (job.nextRunTime | date:'medium') : '—' }}</span></div>
          </div>

          @if (job.cronExpression) {
            <div class="info-card">
              <h3>Schedule</h3>
              <div class="info-row"><span class="info-label">Cron Expression</span><span class="info-value mono">{{ job.cronExpression }}</span></div>
            </div>
          }

          @if (job.payload) {
            <div class="info-card full-width">
              <h3>Payload</h3>
              <pre class="code-block">{{ formatPayload(job.payload) }}</pre>
            </div>
          }

          @if (job.lastError) {
            <div class="info-card full-width error-card">
              <h3>Last Error</h3>
              <pre class="code-block error">{{ job.lastError }}</pre>
            </div>
          }
        </div>

        <div class="section">
          <h2>Execution History</h2>
          @if (historyLoading) {
            <div class="loading-state small">
              <div class="spinner"></div>
            </div>
          } @else if (history.length === 0) {
            <p class="empty-text">No execution history yet.</p>
          } @else {
            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Status</th>
                    <th>Run Time</th>
                    <th>Error</th>
                    <th>Retry</th>
                  </tr>
                </thead>
                <tbody>
                  @for (h of history; track h.id) {
                    <tr>
                      <td class="mono">#{{ h.id }}</td>
                      <td><span class="badge" [class]="'badge badge-' + (h.status || '').toLowerCase()">{{ h.status }}</span></td>
                      <td>{{ h.runTime | date:'medium' }}</td>
                      <td class="error-text">{{ h.errorMessage || '—' }}</td>
                      <td class="mono">{{ h.retryAttempt }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      } @else {
        <div class="empty-state">
          <p>Job not found.</p>
          <a routerLink="/jobs" class="btn btn-primary">Back to Jobs</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .page { max-width: 1000px; }
    .page-header {
      display: flex; align-items: flex-start; justify-content: space-between;
      margin-bottom: 28px; flex-wrap: wrap; gap: 16px;
    }
    .page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); margin: 4px 0 8px; letter-spacing: -0.5px; }
    .back-btn {
      background: none; border: none; cursor: pointer; color: var(--accent);
      font-size: 13px; font-weight: 500; padding: 0;
    }
    .back-btn:hover { text-decoration: underline; }
    .header-actions { display: flex; gap: 8px; }
    .btn {
      padding: 8px 18px; border: none; border-radius: 6px; font-size: 13px; font-weight: 600;
      cursor: pointer; display: inline-flex; align-items: center; gap: 6px; transition: all 0.15s;
      text-decoration: none;
    }
    .btn-primary { background: var(--accent); color: var(--accent-text); }
    .btn-primary:hover { background: var(--accent-hover); }
    .btn-outline { background: transparent; color: var(--text-muted); border: 1px solid var(--border-hover); }
    .btn-outline:hover { border-color: var(--text-muted); color: var(--text-secondary); }
    .btn-danger { background: transparent; color: var(--danger); border: 1px solid var(--danger-border); }
    .btn-danger:hover { background: var(--danger-muted); }
    .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 28px; }
    .info-card {
      background: var(--bg-surface); border-radius: 8px; padding: 20px; border: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
    }
    .info-card.full-width { grid-column: 1 / -1; }
    .info-card.error-card { border-color: var(--danger-border); }
    .info-card h3 {
      font-size: 11px; font-weight: 600; color: var(--text-muted); margin: 0 0 14px;
      text-transform: uppercase; letter-spacing: 1px;
    }
    .info-row {
      display: flex; justify-content: space-between; padding: 8px 0;
      border-bottom: 1px solid var(--border-subtle);
    }
    .info-row:last-child { border: none; }
    .info-label { font-size: 13px; color: var(--text-muted); font-weight: 500; }
    .info-value { font-size: 13px; color: var(--text-secondary); font-weight: 600; }
    .mono { font-family: 'JetBrains Mono', monospace; }
    .code-block {
      background: var(--code-bg); color: var(--text-muted); padding: 16px; border-radius: 6px;
      font-size: 12px; font-family: 'JetBrains Mono', monospace; overflow-x: auto;
      white-space: pre-wrap; word-break: break-all; margin: 8px 0 0; border: 1px solid var(--code-border);
    }
    .code-block.error { color: var(--danger); border-color: var(--danger-border); }
    .badge {
      padding: 3px 10px; border-radius: 4px; font-size: 11px; font-weight: 600;
      text-transform: uppercase; letter-spacing: 0.5px;
    }
    .badge-type { background: var(--badge-type-bg); color: var(--badge-type-text); }
    .badge-pending { background: var(--badge-pending-bg); color: var(--badge-pending-text); }
    .badge-running { background: var(--badge-running-bg); color: var(--badge-running-text); }
    .badge-success { background: var(--badge-success-bg); color: var(--badge-success-text); }
    .badge-failed { background: var(--badge-failed-bg); color: var(--badge-failed-text); }
    .badge-retrying { background: var(--badge-retrying-bg); color: var(--badge-retrying-text); }
    .badge-paused { background: var(--badge-paused-bg); color: var(--badge-paused-text); }
    .badge-dead { background: var(--badge-dead-bg); color: var(--badge-dead-text); }
    .section { background: var(--bg-surface); border-radius: 8px; padding: 24px; border: 1px solid var(--border); box-shadow: var(--shadow-sm); }
    .section h2 { font-size: 16px; font-weight: 600; color: var(--text-primary); margin: 0 0 16px; }
    .table-container { overflow-x: auto; }
    .table { width: 100%; border-collapse: collapse; }
    .table th {
      text-align: left; padding: 10px 14px; font-size: 11px; font-weight: 600;
      color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border);
    }
    .table td { padding: 10px 14px; font-size: 14px; color: var(--text-secondary); border-bottom: 1px solid var(--border-subtle); }
    .table tr:hover td { background: var(--bg-hover); }
    .error-text { color: var(--danger); font-size: 13px; }
    .empty-text { color: var(--text-muted); font-size: 14px; text-align: center; padding: 20px; }
    .loading-state { text-align: center; padding: 60px 20px; color: var(--text-muted); }
    .loading-state.small { padding: 20px; }
    .empty-state {
      text-align: center; padding: 60px 20px; color: var(--text-muted);
      background: var(--bg-surface); border-radius: 8px; border: 1px solid var(--border);
    }
    .spinner {
      width: 28px; height: 28px; border: 2px solid var(--border); border-top-color: var(--accent);
      border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 12px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class JobDetailComponent implements OnInit {
  job: JobResponse | null = null;
  history: JobHistoryResponse[] = [];
  loading = true;
  historyLoading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobService: JobService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.loading = false;
      return;
    }

    this.jobService.getById(id).subscribe({
      next: (job: JobResponse) => {
        this.job = job;
        this.loading = false;
        this.cdr.detectChanges();
        this.loadHistory(id);
      },
      error: () => {
        this.toast.error('Job not found.');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadHistory(jobId: number): void {
    this.jobService.getHistory(jobId).subscribe({
      next: (h: JobHistoryResponse[]) => {
        this.history = h;
        this.historyLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.historyLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  pauseJob(): void {
    if (!this.job) return;
    this.jobService.pause(this.job.id).subscribe({
      next: (j: JobResponse) => {
        this.job = j;
        this.toast.success('Job paused.');
      },
      error: () => this.toast.error('Failed to pause job.')
    });
  }

  resumeJob(): void {
    if (!this.job) return;
    this.jobService.resume(this.job.id).subscribe({
      next: (j: JobResponse) => {
        this.job = j;
        this.toast.success('Job resumed.');
      },
      error: () => this.toast.error('Failed to resume job.')
    });
  }

  deleteJob(): void {
    if (!this.job || !confirm(`Delete "${this.job.name}"?`)) return;
    this.jobService.delete(this.job.id).subscribe({
      next: () => {
        this.toast.success('Job deleted.');
        this.router.navigate(['/jobs']);
      },
      error: () => this.toast.error('Failed to delete job.')
    });
  }

  goBack(): void {
    this.router.navigate(['/jobs']);
  }

  formatPayload(payload: string): string {
    try {
      return JSON.stringify(JSON.parse(payload), null, 2);
    } catch {
      return payload;
    }
  }
}
