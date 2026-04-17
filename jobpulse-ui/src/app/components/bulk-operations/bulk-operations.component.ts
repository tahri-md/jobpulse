import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JobService } from '../../services/job.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-bulk-operations',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bulk-operations-container" *ngIf="selectedJobIds.length > 0">
      <div class="bulk-actions-bar">
        <span class="selection-info">{{ selectedJobIds.length }} job(s) selected</span>
        <div class="action-buttons">
          <button (click)="pauseJobs()" class="btn btn-warning">Pause Selected</button>
          <button (click)="resumeJobs()" class="btn btn-info">Resume Selected</button>
          <button (click)="deleteJobs()" class="btn btn-danger" 
                  [disabled]="isProcessing">Delete Selected</button>
          <button (click)="clearSelection()" class="btn btn-secondary">Clear Selection</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .bulk-operations-container {
      background: var(--bg-secondary);
      padding: 15px 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      border-left: 4px solid var(--primary-color);
    }

    .bulk-actions-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 20px;
    }

    .selection-info {
      font-weight: 600;
      color: var(--text-primary);
    }

    .action-buttons {
      display: flex;
      gap: 10px;
    }

    .btn {
      padding: 8px 16px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
      transition: opacity 0.2s;
    }

    .btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .btn-warning {
      background: var(--warning-color);
      color: white;
    }

    .btn-info {
      background: var(--info-color);
      color: white;
    }

    .btn-danger {
      background: var(--danger-color);
      color: white;
    }

    .btn-secondary {
      background: var(--bg-tertiary);
      color: var(--text-primary);
    }

    .btn:hover:not(:disabled) {
      opacity: 0.8;
    }
  `]
})
export class BulkOperationsComponent {
  @Input() selectedJobIds: number[] = [];
  @Output() refreshJobs = new EventEmitter<void>();

  isProcessing = false;

  constructor(
    private jobService: JobService,
    private toast: ToastService
  ) {}

  pauseJobs(): void {
    if (confirm(`Pause ${this.selectedJobIds.length} jobs?`)) {
      this.isProcessing = true;
      this.jobService.bulkOperation(this.selectedJobIds, 'pause').subscribe({
        next: () => {
          this.toast.success('Jobs paused successfully');
          this.refreshJobs.emit();
          this.clearSelection();
          this.isProcessing = false;
        },
        error: () => {
          this.toast.error('Failed to pause jobs');
          this.isProcessing = false;
        }
      });
    }
  }

  resumeJobs(): void {
    if (confirm(`Resume ${this.selectedJobIds.length} jobs?`)) {
      this.isProcessing = true;
      this.jobService.bulkOperation(this.selectedJobIds, 'resume').subscribe({
        next: () => {
          this.toast.success('Jobs resumed successfully');
          this.refreshJobs.emit();
          this.clearSelection();
          this.isProcessing = false;
        },
        error: () => {
          this.toast.error('Failed to resume jobs');
          this.isProcessing = false;
        }
      });
    }
  }

  deleteJobs(): void {
    if (confirm(`Delete ${this.selectedJobIds.length} jobs? This cannot be undone.`)) {
      this.isProcessing = true;
      this.jobService.bulkOperation(this.selectedJobIds, 'delete').subscribe({
        next: () => {
          this.toast.success('Jobs deleted successfully');
          this.refreshJobs.emit();
          this.clearSelection();
          this.isProcessing = false;
        },
        error: () => {
          this.toast.error('Failed to delete jobs');
          this.isProcessing = false;
        }
      });
    }
  }

  clearSelection(): void {
    this.selectedJobIds = [];
  }
}
