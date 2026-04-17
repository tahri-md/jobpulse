import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JobService } from '../../../services/job.service';
import { ToastService } from '../../../services/toast.service';
import { JobResponse } from '../../../models/job.model';
import { Status } from '../../../models/enums';

@Component({
  selector: 'app-job-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="search-filter-container">
      <div class="search-section">
        <input
          type="text"
          [(ngModel)]="searchQuery"
          placeholder="Search jobs by name..."
          class="search-input"
          (keyup.enter)="search()"
        />
        <button (click)="search()" class="btn btn-primary">Search</button>
        <button (click)="clearSearch()" class="btn btn-secondary">Clear</button>
      </div>

      <div class="filter-section">
        <select [(ngModel)]="selectedStatus" (change)="filterByStatus()" class="filter-select">
          <option value="">All Statuses</option>
          <option [value]="Status.PENDING">Pending</option>
          <option [value]="Status.RUNNING">Running</option>
          <option [value]="Status.SUCCESS">Success</option>
          <option [value]="Status.FAILED">Failed</option>
          <option [value]="Status.RETRYING">Retrying</option>
          <option [value]="Status.PAUSED">Paused</option>
        </select>
      </div>

      <div class="date-filter-section">
        <input
          type="date"
          [(ngModel)]="startDate"
          class="date-input"
          placeholder="Start Date"
        />
        <input
          type="date"
          [(ngModel)]="endDate"
          class="date-input"
          placeholder="End Date"
        />
        <button (click)="filterByDateRange()" class="btn btn-primary">Filter</button>
      </div>
    </div>

    <div class="results-section" *ngIf="searchResults && searchResults.length > 0">
      <h3>Search Results ({{ searchResults.length }} jobs)</h3>
      <div class="job-list">
        <div *ngFor="let job of searchResults" class="job-item">
          <div class="job-header">
            <h4>{{ job.name }}</h4>
            <span class="status" [ngClass]="'status-' + job.status.toLowerCase()">
              {{ job.status }}
            </span>
          </div>
          <div class="job-details">
            <p><strong>Type:</strong> {{ job.jobType }}</p>
            <p><strong>Next Run:</strong> {{ job.nextRunTime | date }}</p>
            <p><strong>Retries:</strong> {{ job.retryCount }} / {{ job.maxRetries }}</p>
          </div>
          <button (click)="viewJob(job.id)" class="btn btn-sm btn-info">View</button>
        </div>
      </div>
    </div>

    <div class="no-results" *ngIf="searched && (!searchResults || searchResults.length === 0)">
      <p>No jobs found matching your criteria.</p>
    </div>
  `,
  styles: [`
    .search-filter-container {
      padding: 20px;
      background: var(--bg-secondary);
      border-radius: 8px;
      margin-bottom: 20px;
      display: grid;
      gap: 15px;
    }

    .search-section, .filter-section, .date-filter-section {
      display: flex;
      gap: 10px;
      align-items: center;
    }

    .search-input, .filter-select, .date-input {
      padding: 8px 12px;
      border: 1px solid var(--border-color);
      border-radius: 4px;
      font-size: 14px;
      background: var(--bg-primary);
      color: var(--text-primary);
    }

    .search-input {
      flex: 1;
      min-width: 200px;
    }

    .filter-select {
      min-width: 150px;
    }

    .date-input {
      width: 150px;
    }

    .btn {
      padding: 8px 16px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
    }

    .btn-primary {
      background: var(--primary-color);
      color: white;
    }

    .btn-secondary {
      background: var(--bg-tertiary);
      color: var(--text-primary);
    }

    .btn-info {
      background: var(--info-color);
      color: white;
    }

    .results-section {
      margin-top: 20px;
    }

    .job-list {
      display: grid;
      gap: 15px;
    }

    .job-item {
      background: var(--bg-secondary);
      padding: 15px;
      border-radius: 8px;
      border-left: 4px solid var(--primary-color);
    }

    .job-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
    }

    .job-header h4 {
      margin: 0;
    }

    .status {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
    }

    .status-pending { background: var(--warning-color); }
    .status-running { background: var(--info-color); }
    .status-success { background: var(--success-color); }
    .status-failed { background: var(--danger-color); }
    .status-retrying { background: var(--warning-color); }
    .status-paused { background: var(--secondary-color); }

    .job-details {
      font-size: 13px;
      color: var(--text-secondary);
      margin-bottom: 10px;
    }

    .job-details p {
      margin: 5px 0;
    }

    .no-results {
      text-align: center;
      padding: 40px 20px;
      color: var(--text-secondary);
    }
  `]
})
export class JobSearchComponent implements OnInit {
  searchQuery = '';
  selectedStatus: Status | '' = '';
  startDate: string = '';
  endDate: string = '';
  searchResults: JobResponse[] = [];
  searched = false;
  Status = Status;

  constructor(
    private jobService: JobService,
    private toast: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {}

  search(): void {
    if (!this.searchQuery.trim()) {
      this.toast.warning('Please enter a search query');
      return;
    }

    this.jobService.search(this.searchQuery).subscribe({
      next: (results) => {
        this.searchResults = results;
        this.searched = true;
      },
      error: () => this.toast.error('Failed to search jobs')
    });
  }

  filterByStatus(): void {
    if (!this.selectedStatus) {
      this.clearSearch();
      return;
    }

    this.jobService.filterByStatus(this.selectedStatus).subscribe({
      next: (results) => {
        this.searchResults = results;
        this.searched = true;
      },
      error: () => this.toast.error('Failed to filter jobs')
    });
  }

  filterByDateRange(): void {
    if (!this.startDate || !this.endDate) {
      this.toast.warning('Please select both start and end dates');
      return;
    }

    this.jobService.filterByDateRange(this.startDate, this.endDate).subscribe({
      next: (results) => {
        this.searchResults = results;
        this.searched = true;
      },
      error: () => this.toast.error('Failed to filter jobs by date')
    });
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.selectedStatus = '';
    this.startDate = '';
    this.endDate = '';
    this.searchResults = [];
    this.searched = false;
  }

  viewJob(jobId: number): void {
    this.router.navigate(['/jobs', jobId]);
  }
}
