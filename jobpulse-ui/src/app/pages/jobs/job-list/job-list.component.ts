import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { JobService } from '../../../services/job.service';
import { ToastService } from '../../../services/toast.service';
import { JobResponse, Status } from '../../../models';

@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Jobs</h1>
          <p class="subtitle">Manage and monitor your jobs</p>
        </div>
        <a routerLink="/jobs/create" class="btn btn-primary">+ New Job</a>
      </div>

      <div class="filters">
        <input
          type="text"
          placeholder="Search jobs..."
          [(ngModel)]="searchTerm"
          (ngModelChange)="applyFilters()"
          class="search-input"
        />
        <select [(ngModel)]="statusFilter" (ngModelChange)="applyFilters()" class="filter-select">
          <option value="">All Statuses</option>
          <option value="PENDING">Pending</option>
          <option value="RUNNING">Running</option>
          <option value="SUCCESS">Success</option>
          <option value="RETRYING">Retrying</option>
          <option value="PAUSED">Paused</option>
        </select>
        <select [(ngModel)]="typeFilter" (ngModelChange)="applyFilters()" class="filter-select">
          <option value="">All Types</option>
          <option value="EMAIL">Email</option>
          <option value="HTTP_CALL">HTTP</option>
          <option value="DATA_CLEANUP">Data Cleanup</option>
          <option value="REPORT_GENERATION">Report Generation</option>
          <option value="SCRIPT">Script</option>
          <option value="LOG">Log</option>
        </select>
      </div>

      @if (loading) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Loading...</p>
        </div>
      } @else if (filteredJobs.length === 0) {
        <div class="empty-state">
          <p>{{ allJobs.length === 0 ? 'No jobs yet.' : 'No jobs match your filters.' }}</p>
          @if (allJobs.length === 0) {
            <a routerLink="/jobs/create" class="btn btn-primary">Create your first job</a>
          }
        </div>
      } @else {
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Type</th>
                <th>Status</th>
                <th>Recurring</th>
                <th>Retries</th>
                <th>Next Run</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (job of filteredJobs; track job.id) {
                <tr>
                  <td class="id-cell">#{{ job.id }}</td>
                  <td class="job-name">{{ job.name }}</td>
                  <td><span class="badge badge-type">{{ job.jobType }}</span></td>
                  <td><span class="badge" [class]="'badge badge-' + (job.status || '').toLowerCase()">{{ job.status }}</span></td>
                  <td class="mono">{{ job.recurring ? 'Yes' : '—' }}</td>
                  <td class="mono">{{ job.retryCount }}/{{ job.maxRetries }}</td>
                  <td>{{ job.nextRunTime ? (job.nextRunTime | date:'medium') : '—' }}</td>
                  <td class="actions-cell">
                    <a [routerLink]="['/jobs', job.id]" class="action-btn" title="View details">View</a>
                    @if (job.status === 'PENDING' || job.status === 'RUNNING' || job.status === 'RETRYING') {
                      <button class="action-btn" (click)="pauseJob(job)" title="Pause">Pause</button>
                    }
                    @if (job.status === 'PAUSED') {
                      <button class="action-btn accent" (click)="resumeJob(job)" title="Resume">Resume</button>
                    }
                    <button class="action-btn danger" (click)="deleteJob(job)" title="Delete">Delete</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: [`
    .page { max-width: 1200px; }
    .page-header {
      display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px;
    }
    .page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); margin: 0; letter-spacing: -0.5px; }
    .subtitle { font-size: 14px; color: var(--text-muted); margin: 4px 0 0; }
    .btn {
      padding: 9px 20px; border: none; border-radius: 6px; font-size: 13px; font-weight: 600;
      cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 6px; transition: all 0.15s;
    }
    .btn-primary { background: var(--accent); color: var(--accent-text); }
    .btn-primary:hover { background: var(--accent-hover); }
    .filters { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
    .search-input, .filter-select {
      padding: 9px 14px; border: 1px solid var(--border); border-radius: 6px; font-size: 14px;
      outline: none; transition: border-color 0.15s; background: var(--bg-surface); color: var(--text-secondary);
    }
    .search-input { flex: 1; min-width: 200px; }
    .search-input:focus, .filter-select:focus { border-color: var(--accent); }
    .search-input::placeholder { color: var(--text-placeholder); }
    .table-container {
      background: var(--bg-surface); border-radius: 8px; overflow-x: auto; border: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
    }
    .table { width: 100%; border-collapse: collapse; }
    .table th {
      text-align: left; padding: 12px 14px; font-size: 11px; font-weight: 600; color: var(--text-muted);
      text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border);
    }
    .table td { padding: 12px 14px; font-size: 14px; color: var(--text-secondary); border-bottom: 1px solid var(--border-subtle); }
    .table tr:hover td { background: var(--bg-hover); }
    .id-cell { font-size: 13px; color: var(--text-muted); font-weight: 500; font-family: 'JetBrains Mono', monospace; }
    .job-name { font-weight: 600; color: var(--text-primary); }
    .mono { font-family: 'JetBrains Mono', monospace; font-size: 13px; color: var(--text-muted); }
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
    .actions-cell { display: flex; gap: 6px; align-items: center; }
    .action-btn {
      background: none; border: 1px solid var(--border-hover); cursor: pointer; font-size: 12px;
      padding: 4px 10px; border-radius: 4px; transition: all 0.15s; text-decoration: none;
      color: var(--text-muted); font-weight: 500;
    }
    .action-btn:hover { border-color: var(--text-muted); color: var(--text-secondary); background: var(--bg-elevated); }
    .action-btn.accent { color: var(--accent); border-color: var(--accent-muted); }
    .action-btn.accent:hover { background: var(--accent-muted); border-color: var(--accent); }
    .action-btn.danger { color: var(--danger); border-color: var(--danger-muted); }
    .action-btn.danger:hover { background: var(--danger-muted); border-color: var(--danger); }
    .loading-state, .empty-state {
      text-align: center; padding: 60px 20px; color: var(--text-muted); background: var(--bg-surface);
      border-radius: 8px; border: 1px solid var(--border);
    }
    .spinner {
      width: 28px; height: 28px; border: 2px solid var(--border); border-top-color: var(--accent);
      border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 12px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class JobListComponent implements OnInit {
  allJobs: JobResponse[] = [];
  filteredJobs: JobResponse[] = [];
  loading = true;
  searchTerm = '';
  statusFilter = '';
  typeFilter = '';

  constructor(
    private jobService: JobService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.loading = true;
    this.jobService.getAll().subscribe({
      next: (jobs: JobResponse[]) => {
        this.allJobs = jobs;
        this.applyFilters();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load jobs:', err);
        this.toast.error('Failed to load jobs.');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    this.filteredJobs = this.allJobs.filter(job => {
      const matchSearch = !this.searchTerm ||
        (job.name || '').toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (job.jobType || '').toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchStatus = !this.statusFilter || job.status === this.statusFilter;
      const matchType = !this.typeFilter || job.jobType === this.typeFilter;
      return matchSearch && matchStatus && matchType;
    });
  }

  pauseJob(job: JobResponse): void {
    this.jobService.pause(job.id).subscribe({
      next: () => {
        this.toast.success(`Job "${job.name}" paused.`);
        this.loadJobs();
      },
      error: () => this.toast.error('Failed to pause job.')
    });
  }

  resumeJob(job: JobResponse): void {
    this.jobService.resume(job.id).subscribe({
      next: () => {
        this.toast.success(`Job "${job.name}" resumed.`);
        this.loadJobs();
      },
      error: () => this.toast.error('Failed to resume job.')
    });
  }

  deleteJob(job: JobResponse): void {
    if (!confirm(`Are you sure you want to delete "${job.name}"?`)) return;
    this.jobService.delete(job.id).subscribe({
      next: () => {
        this.toast.success(`Job "${job.name}" deleted.`);
        this.loadJobs();
      },
      error: () => this.toast.error('Failed to delete job.')
    });
  }
}
