import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { JobService } from '../../services/job.service';
import { AuthService } from '../../services/auth.service';
import { JobStatsResponse, JobResponse, Status } from '../../models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="dashboard">
      <div class="page-header">
        <div>
          <h1>Dashboard</h1>
          <p class="subtitle">Welcome back, {{ auth.currentUser()?.username }}</p>
        </div>
        <a routerLink="/jobs/create" class="btn btn-primary">+ New Job</a>
      </div>

      @if (stats) {
        <div class="stats-grid">
          <div class="stat-card">
            <span class="stat-value">{{ stats.totalJobs }}</span>
            <span class="stat-label">Total</span>
          </div>
          <div class="stat-card">
            <span class="stat-value">{{ stats.pendingJobs }}</span>
            <span class="stat-label">Pending</span>
          </div>
          <div class="stat-card">
            <span class="stat-value">{{ stats.runningJobs }}</span>
            <span class="stat-label">Running</span>
          </div>
          <div class="stat-card accent">
            <span class="stat-value">{{ stats.successfulJobs }}</span>
            <span class="stat-label">Succeeded</span>
          </div>
          <div class="stat-card">
            <span class="stat-value">{{ stats.failedJobs }}</span>
            <span class="stat-label">Failed</span>
          </div>
          <div class="stat-card">
            <span class="stat-value">{{ stats.retryingJobs }}</span>
            <span class="stat-label">Retrying</span>
          </div>
          <div class="stat-card">
            <span class="stat-value">{{ stats.deadLetterJobs }}</span>
            <span class="stat-label">Dead</span>
          </div>
        </div>
      }

      <div class="section">
        <div class="section-header">
          <h2>Recent Jobs</h2>
          <a routerLink="/jobs" class="link">View all &rarr;</a>
        </div>

        @if (loading) {
          <div class="loading-state">
            <div class="spinner"></div>
            <p>Loading...</p>
          </div>
        } @else if (recentJobs.length === 0) {
          <div class="empty-state">
            <p>No jobs yet. Create your first job to get started.</p>
            <a routerLink="/jobs/create" class="btn btn-primary">Create Job</a>
          </div>
        } @else {
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Retries</th>
                  <th>Next Run</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (job of recentJobs; track job.id) {
                  <tr>
                    <td class="job-name">{{ job.name }}</td>
                    <td><span class="badge badge-type">{{ job.jobType }}</span></td>
                    <td><span class="badge" [class]="'badge badge-' + (job.status || '').toLowerCase()">{{ job.status }}</span></td>
                    <td class="mono">{{ job.retryCount }}/{{ job.maxRetries }}</td>
                    <td>{{ job.nextRunTime ? (job.nextRunTime | date:'short') : '—' }}</td>
                    <td>
                      <a [routerLink]="['/jobs', job.id]" class="action-link">View</a>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .dashboard { max-width: 1200px; }
    .page-header {
      display: flex; align-items: center; justify-content: space-between; margin-bottom: 32px;
    }
    .page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); margin: 0; letter-spacing: -0.5px; }
    .subtitle { font-size: 14px; color: var(--text-muted); margin: 4px 0 0; }
    .btn {
      padding: 9px 20px; border: none; border-radius: 6px; font-size: 13px; font-weight: 600;
      cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 6px; transition: all 0.15s;
    }
    .btn-primary { background: var(--accent); color: var(--accent-text); }
    .btn-primary:hover { background: var(--accent-hover); }
    .stats-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 12px; margin-bottom: 32px;
    }
    .stat-card {
      background: var(--bg-surface); border-radius: 8px; padding: 20px; border: 1px solid var(--border);
      display: flex; flex-direction: column; gap: 4px; transition: border-color 0.15s;
      box-shadow: var(--shadow-sm);
    }
    .stat-card:hover { border-color: var(--border-hover); }
    .stat-card.accent { border-color: var(--accent); }
    .stat-card.accent .stat-value { color: var(--accent); }
    .stat-value { font-size: 28px; font-weight: 700; color: var(--text-primary); font-variant-numeric: tabular-nums; }
    .stat-label { font-size: 12px; color: var(--text-muted); font-weight: 500; text-transform: uppercase; letter-spacing: 0.5px; }
    .section { background: var(--bg-surface); border-radius: 8px; padding: 24px; border: 1px solid var(--border); box-shadow: var(--shadow-sm); }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
    .section-header h2 { font-size: 16px; font-weight: 600; color: var(--text-primary); margin: 0; }
    .link { color: var(--accent); text-decoration: none; font-size: 13px; font-weight: 500; }
    .link:hover { text-decoration: underline; }
    .table-container { overflow-x: auto; }
    .table { width: 100%; border-collapse: collapse; }
    .table th {
      text-align: left; padding: 10px 14px; font-size: 11px; font-weight: 600; color: var(--text-muted);
      text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border);
    }
    .table td { padding: 12px 14px; font-size: 14px; color: var(--text-secondary); border-bottom: 1px solid var(--border-subtle); }
    .table tr:hover td { background: var(--bg-hover); }
    .job-name { font-weight: 600; color: var(--text-primary); }
    .mono { font-family: 'JetBrains Mono', monospace; font-size: 13px; }
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
    .action-link { color: var(--accent); text-decoration: none; font-weight: 500; font-size: 13px; }
    .action-link:hover { text-decoration: underline; }
    .loading-state, .empty-state { text-align: center; padding: 40px 20px; color: var(--text-muted); }
    .spinner {
      width: 28px; height: 28px; border: 2px solid var(--border); border-top-color: var(--accent);
      border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 12px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class DashboardComponent implements OnInit {
  stats: JobStatsResponse | null = null;
  recentJobs: JobResponse[] = [];
  loading = true;

  constructor(
    private jobService: JobService,
    public auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    this.jobService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.cdr.detectChanges();
      },
      error: () => {}
    });

    this.jobService.getAll().subscribe({
      next: (jobs) => {
        this.recentJobs = jobs.slice(0, 10);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
