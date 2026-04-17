import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { JobTemplateService, JobTemplate, JobTemplateRequest } from '../../../services/job-template.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-template-edit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <button class="back-btn" (click)="goBack()">&larr; Back to Templates</button>
          <h1>Edit Template</h1>
          <p class="subtitle">Update template configuration</p>
        </div>
      </div>

      @if (loading) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Loading template...</p>
        </div>
      } @else {
        <form (ngSubmit)="onSubmit()" class="form-container">
          <div class="form-section">
            <h2>Template Details</h2>

            <div class="form-group">
              <label for="name">Template Name *</label>
              <input 
                id="name"
                [(ngModel)]="template.name" 
                name="name" 
                placeholder="e.g., Daily Email Report"
                required 
              />
            </div>

            <div class="form-group">
              <label for="description">Description</label>
              <textarea 
                id="description"
                [(ngModel)]="template.description" 
                name="description"
                placeholder="What does this template do?"
              ></textarea>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label for="jobType">Job Type *</label>
                <select [(ngModel)]="template.jobType" id="jobType" name="jobType" required>
                  <option value="">Select a type</option>
                  <option value="EMAIL">Email</option>
                  <option value="HTTP_CALL">HTTP Call</option>
                  <option value="DATA_CLEANUP">Data Cleanup</option>
                  <option value="REPORT_GENERATION">Report Generation</option>
                  <option value="SCRIPT">Script</option>
                  <option value="LOG">Log</option>
                </select>
              </div>
              <div class="form-group">
                <label for="maxRetries">Max Retries</label>
                <input 
                  id="maxRetries"
                  [(ngModel)]="template.maxRetries" 
                  name="maxRetries" 
                  type="number" 
                  min="0"
                  max="10"
                />
              </div>
            </div>

            <div class="form-group">
              <label for="cronExpression">Cron Expression</label>
              <input 
                id="cronExpression"
                [(ngModel)]="template.cronExpression" 
                name="cronExpression"
                placeholder="e.g., 0 9 * * * (daily at 9am)"
              />
            </div>

            <div class="form-group">
              <label for="payload">Payload (JSON)</label>
              <textarea 
                id="payload"
                [(ngModel)]="template.payload" 
                name="payload"
                placeholder="Enter job configuration as JSON"
                class="mono"
              ></textarea>
            </div>

            <div class="form-group checkbox">
              <input 
                id="isPublic"
                [(ngModel)]="template.isPublic" 
                name="isPublic" 
                type="checkbox" 
              />
              <label for="isPublic" class="checkbox-label">
                Make this template public (other users can use it)
              </label>
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" [disabled]="submitting">
              {{ submitting ? 'Saving...' : 'Save Changes' }}
            </button>
            <button type="button" class="btn btn-secondary" (click)="goBack()" [disabled]="submitting">
              Cancel
            </button>
          </div>
        </form>
      }
    </div>
  `,
  styles: [`
    .page {
      max-width: 900px;
      margin: 0 auto;
      padding: 24px;
    }

    .page-header {
      margin-bottom: 32px;
    }

    .back-btn {
      background: none;
      border: none;
      color: var(--accent);
      font-size: 14px;
      cursor: pointer;
      font-weight: 600;
      padding: 0;
      margin-bottom: 8px;
      transition: color 0.2s;
    }

    .back-btn:hover {
      color: var(--accent-hover);
    }

    .page-header h1 {
      font-size: 28px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 8px 0 6px;
      letter-spacing: -0.5px;
    }

    .subtitle {
      font-size: 14px;
      color: var(--text-muted);
      margin: 0;
    }

    .loading-state {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-muted);
      background: var(--bg-surface);
      border-radius: 8px;
      border: 1px solid var(--border);
    }

    .spinner {
      width: 28px;
      height: 28px;
      border: 2px solid var(--border);
      border-top-color: var(--accent);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 12px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .form-container {
      background: var(--bg-surface);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 24px;
      box-shadow: var(--shadow-sm);
    }

    .form-section {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .form-section h2 {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0 0 8px;
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .form-group label {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .form-group input,
    .form-group textarea,
    .form-group select {
      padding: 10px 12px;
      border: 1px solid var(--border);
      border-radius: 6px;
      background: var(--bg-primary);
      color: var(--text-primary);
      font-size: 14px;
      font-family: inherit;
      transition: border-color 0.2s;
    }

    .form-group input:focus,
    .form-group textarea:focus,
    .form-group select:focus {
      outline: none;
      border-color: var(--accent);
      box-shadow: 0 0 0 3px var(--accent-muted);
    }

    .form-group textarea {
      resize: vertical;
      min-height: 100px;
    }

    .form-group textarea.mono {
      font-family: 'JetBrains Mono', monospace;
      font-size: 12px;
    }

    .form-group.checkbox {
      flex-direction: row;
      align-items: center;
      gap: 8px;
    }

    .form-group.checkbox input {
      width: 18px;
      height: 18px;
      cursor: pointer;
    }

    .checkbox-label {
      font-size: 14px;
      font-weight: 500;
      color: var(--text-secondary);
      text-transform: none;
      cursor: pointer;
      letter-spacing: normal;
    }

    .form-actions {
      display: flex;
      gap: 12px;
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid var(--border);
    }

    .btn {
      padding: 10px 24px;
      border: none;
      border-radius: 6px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      text-decoration: none;
    }

    .btn-primary {
      background: var(--accent);
      color: var(--accent-text);
    }

    .btn-primary:hover:not(:disabled) {
      background: var(--accent-hover);
    }

    .btn-secondary {
      background: var(--bg-elevated);
      color: var(--text-secondary);
      border: 1px solid var(--border);
    }

    .btn-secondary:hover:not(:disabled) {
      background: var(--bg-hover);
      color: var(--text-primary);
    }

    .btn:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    @media (max-width: 768px) {
      .form-row {
        grid-template-columns: 1fr;
      }

      .form-actions {
        flex-direction: column;
      }
    }
  `]
})
export class TemplateEditComponent implements OnInit {
  template: JobTemplateRequest = {
    name: '',
    description: '',
    jobType: '',
    payload: '{}',
    cronExpression: '',
    maxRetries: 3,
    isPublic: false
  };

  loading = true;
  submitting = false;
  private templateId: number = 0;

  constructor(
    private templateService: JobTemplateService,
    private toast: ToastService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.templateId = parseInt(params['id']);
      this.loadTemplate();
    });
  }

  private loadTemplate(): void {
    this.templateService.getById(this.templateId).subscribe({
      next: (data) => {
        this.template = {
          name: data.name,
          description: data.description,
          jobType: data.jobType,
          payload: data.payload,
          cronExpression: data.cronExpression,
          maxRetries: data.maxRetries,
          isPublic: data.isPublic
        };
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load template:', err);
        this.toast.error('Failed to load template.');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onSubmit(): void {
    if (!this.template.name || !this.template.jobType) {
      this.toast.warning('Please fill in required fields (Name, Job Type)');
      return;
    }

    this.submitting = true;
    this.templateService.update(this.templateId, this.template).subscribe({
      next: () => {
        this.toast.success('Template updated successfully!');
        this.router.navigate(['/templates']);
      },
      error: (err) => {
        console.error('Failed to update template:', err);
        this.submitting = false;
        this.toast.error('Failed to update template.');
        this.cdr.detectChanges();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/templates']);
  }
}
