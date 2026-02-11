import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JobService } from '../../services/job.service';
import { ToastService } from '../../services/toast.service';
import { DeadLetterJobResponse } from '../../models';

@Component({
  selector: 'app-dead-letter',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Dead Letter Queue</h1>
          <p class="subtitle">Jobs that permanently failed after exhausting all retries</p>
        </div>
      </div>

      @if (loading) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Loading...</p>
        </div>
      } @else if (deadLetterJobs.length === 0) {
        <div class="empty-state">
          <h3>All Clear</h3>
          <p>No dead letter jobs. Everything is running smoothly.</p>
        </div>
      } @else {
        <div class="cards-grid">
          @for (dlj of deadLetterJobs; track dlj.id) {
            <div class="dl-card">
              <div class="dl-header">
                <span class="dl-id mono">#{{ dlj.id }}</span>
                <span class="badge badge-dead">DEAD</span>
              </div>
              <div class="dl-body">
                <div class="dl-row">
                  <span class="dl-label">Job Name</span>
                  <span class="dl-value">{{ dlj.jobName }}</span>
                </div>
                <div class="dl-row">
                  <span class="dl-label">Job Type</span>
                  <span class="dl-value"><span class="badge badge-type">{{ dlj.jobType }}</span></span>
                </div>
                <div class="dl-row">
                  <span class="dl-label">Failed At</span>
                  <span class="dl-value">{{ dlj.failedAt | date:'medium' }}</span>
                </div>
                <div class="dl-row">
                  <span class="dl-label">Retries</span>
                  <span class="dl-value mono">{{ dlj.retryCount }}/{{ dlj.maxRetries }}</span>
                </div>
                @if (dlj.lastError) {
                  <div class="dl-error">
                    <span class="dl-label">Error</span>
                    <pre>{{ dlj.lastError }}</pre>
                  </div>
                }
              </div>
              <div class="dl-footer">
                <button class="btn btn-primary" (click)="replay(dlj)" [disabled]="dlj.id === replayingId">
                  {{ dlj.id === replayingId ? 'Replaying...' : 'Replay' }}
                </button>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .page { max-width: 1000px; }
    .page-header { margin-bottom: 28px; }
    .page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); margin: 0 0 4px; letter-spacing: -0.5px; }
    .subtitle { font-size: 14px; color: var(--text-muted); margin: 0; }
    .cards-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(400px, 1fr)); gap: 16px; }
    .dl-card { background: var(--bg-surface); border-radius: 8px; border: 1px solid var(--border); overflow: hidden; box-shadow: var(--shadow-sm); }
    .dl-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 14px 20px; border-bottom: 1px solid var(--border-subtle);
    }
    .dl-id { font-size: 14px; font-weight: 700; color: var(--text-secondary); }
    .mono { font-family: 'JetBrains Mono', monospace; }
    .dl-body { padding: 16px 20px; }
    .dl-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: 8px 0; border-bottom: 1px solid var(--border-subtle);
    }
    .dl-label { font-size: 13px; color: var(--text-muted); font-weight: 500; }
    .dl-value { font-size: 13px; color: var(--text-secondary); font-weight: 600; }
    .dl-error { margin-top: 12px; }
    .dl-error pre {
      background: var(--code-bg); color: var(--danger); padding: 12px; border-radius: 6px;
      font-size: 12px; overflow-x: auto; white-space: pre-wrap; word-break: break-all;
      margin: 6px 0 0; border: 1px solid var(--danger-border);
      font-family: 'JetBrains Mono', monospace;
    }
    .dl-footer {
      padding: 12px 20px; border-top: 1px solid var(--border-subtle); display: flex; justify-content: flex-end;
    }
    .badge {
      padding: 3px 10px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: uppercase;
    }
    .badge-dead { background: var(--badge-dead-bg); color: var(--badge-dead-text); }
    .badge-type { background: var(--badge-type-bg); color: var(--badge-type-text); }
    .btn {
      padding: 8px 18px; border: none; border-radius: 6px; font-size: 13px; font-weight: 600;
      cursor: pointer; transition: all 0.15s;
    }
    .btn-primary { background: var(--accent); color: var(--accent-text); }
    .btn-primary:hover:not(:disabled) { background: var(--accent-hover); }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .loading-state, .empty-state {
      text-align: center; padding: 60px 20px; color: var(--text-muted); background: var(--bg-surface);
      border-radius: 8px; border: 1px solid var(--border);
    }
    .empty-state h3 { color: var(--text-secondary); margin: 0 0 4px; }
    .spinner {
      width: 28px; height: 28px; border: 2px solid var(--border); border-top-color: var(--accent);
      border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 12px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class DeadLetterComponent implements OnInit {
  deadLetterJobs: DeadLetterJobResponse[] = [];
  loading = true;
  replayingId: number | null = null;

  constructor(
    private jobService: JobService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.jobService.getDeadLetterJobs().subscribe({
      next: (jobs) => {
        this.deadLetterJobs = jobs;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.toast.error('Failed to load dead letter jobs.');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  replay(dlj: DeadLetterJobResponse): void {
    this.replayingId = dlj.id;
    this.jobService.replayDeadLetter(dlj.id).subscribe({
      next: () => {
        this.toast.success('Job replayed successfully!');
        this.replayingId = null;
        this.load();
      },
      error: () => {
        this.toast.error('Failed to replay job.');
        this.replayingId = null;
      }
    });
  }

  formatPayload(payload: string): string {
    try {
      return JSON.stringify(JSON.parse(payload), null, 2);
    } catch {
      return payload;
    }
  }
}
