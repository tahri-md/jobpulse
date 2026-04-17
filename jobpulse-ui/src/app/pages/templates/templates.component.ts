import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { JobTemplateService, JobTemplate, JobTemplateRequest } from '../../services/job-template.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-job-templates',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Job Templates</h1>
          <p class="subtitle">Reusable job configurations that save setup time</p>
        </div>
        <button class="btn btn-primary" (click)="toggleCreateForm()" [class.active]="showCreateForm">
          {{ showCreateForm ? 'Cancel' : '+ New Template' }}
        </button>
      </div>

      @if (showCreateForm) {
        <div class="create-form">
          <h2>Create New Template</h2>
          <form (ngSubmit)="createTemplate()" class="form-layout">
            <div class="form-row">
              <div class="form-group">
                <label for="name">Template Name *</label>
                <input 
                  id="name"
                  [(ngModel)]="newTemplate.name" 
                  name="name" 
                  placeholder="e.g., Daily Email Report"
                  required 
                />
              </div>
              <div class="form-group">
                <label for="jobType">Job Type *</label>
                <select [(ngModel)]="newTemplate.jobType" id="jobType" name="jobType" required>
                  <option value="">Select a type</option>
                  <option value="EMAIL">Email</option>
                  <option value="HTTP_CALL">HTTP Call</option>
                  <option value="DATA_CLEANUP">Data Cleanup</option>
                  <option value="REPORT_GENERATION">Report Generation</option>
                  <option value="SCRIPT">Script</option>
                  <option value="LOG">Log</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label for="description">Description</label>
              <textarea 
                id="description"
                [(ngModel)]="newTemplate.description" 
                name="description"
                placeholder="What does this template do?"
              ></textarea>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label for="maxRetries">Max Retries</label>
                <input 
                  id="maxRetries"
                  [(ngModel)]="newTemplate.maxRetries" 
                  name="maxRetries" 
                  type="number" 
                  min="0"
                  max="10"
                />
              </div>
              <div class="form-group">
                <label for="cronExpression">Cron Expression</label>
                <input 
                  id="cronExpression"
                  [(ngModel)]="newTemplate.cronExpression" 
                  name="cronExpression"
                  placeholder="e.g., 0 9 * * * (daily at 9am)"
                />
              </div>
            </div>

            <div class="form-group">
              <label for="payload">Payload (JSON)</label>
              <textarea 
                id="payload"
                [(ngModel)]="newTemplate.payload" 
                name="payload"
                placeholder="Enter job configuration as JSON"
                class="mono"
              ></textarea>
            </div>

            <div class="form-group checkbox">
              <input 
                id="isPublic"
                [(ngModel)]="newTemplate.isPublic" 
                name="isPublic" 
                type="checkbox" 
              />
              <label for="isPublic" class="checkbox-label">
                Make this template public (other users can use it)
              </label>
            </div>

            <div class="form-actions">
              <button type="submit" class="btn btn-success">Save Template</button>
              <button type="button" class="btn btn-secondary" (click)="showCreateForm = false">Cancel</button>
            </div>
          </form>
        </div>
      }

      <div class="templates-section">
        <div class="section-header">
          <h2>Available Templates</h2>
          <span class="count">{{ filteredTemplates.length }} template(s)</span>
        </div>

        @if (loading) {
          <div class="loading-state">
            <div class="spinner"></div>
            <p>Loading templates...</p>
          </div>
        } @else if (templates.length === 0) {
          <div class="empty-state">
            <p>No templates available yet</p>
            <button class="btn btn-primary" (click)="showCreateForm = true">Create your first template</button>
          </div>
        } @else {
          <div class="templates-grid">
            @for (template of filteredTemplates; track template.id) {
              <div class="template-card">
                <div class="card-header">
                  <div class="card-title">
                    <h3>{{ template.name }}</h3>
                    <span class="badge" [class.badge-public]="template.isPublic" [class.badge-private]="!template.isPublic">
                      {{ template.isPublic ? 'Public' : 'Private' }}
                    </span>
                  </div>
                  <span class="owner-badge">by {{ template.ownerName }}</span>
                </div>

                @if (template.description) {
                  <p class="card-description">{{ template.description }}</p>
                }

                <div class="card-details">
                  <div class="detail-item">
                    <span class="detail-label">Type:</span>
                    <span class="detail-value badge badge-type">{{ template.jobType }}</span>
                  </div>
                  <div class="detail-item">
                    <span class="detail-label">Max Retries:</span>
                    <span class="detail-value">{{ template.maxRetries }}</span>
                  </div>
                  @if (template.cronExpression) {
                    <div class="detail-item">
                      <span class="detail-label">Schedule:</span>
                      <span class="detail-value mono">{{ template.cronExpression }}</span>
                    </div>
                  }
                </div>

                <div class="card-actions">
                  <button class="action-btn primary" (click)="useTemplate(template.id)" title="Use this template">Use Template</button>
                  @if (canEditTemplate(template)) {
                    <button class="action-btn" (click)="editTemplate(template.id)" title="Edit template">Edit</button>
                  }
                  @if (canDeleteTemplate(template)) {
                    <button class="action-btn danger" (click)="deleteTemplate(template.id)" title="Delete template">Delete</button>
                  }
                </div>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .page {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: start;
      margin-bottom: 32px;
    }

    .page-header h1 {
      font-size: 28px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0 0 6px;
      letter-spacing: -0.5px;
    }

    .subtitle {
      font-size: 14px;
      color: var(--text-muted);
      margin: 0;
    }

    .btn {
      padding: 10px 20px;
      border: none;
      border-radius: 6px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }

    .btn-primary {
      background: var(--accent);
      color: var(--accent-text);
    }

    .btn-primary:hover {
      background: var(--accent-hover);
    }

    .btn-primary.active {
      background: var(--accent-hover);
    }

    .btn-success {
      background: var(--success);
      color: white;
    }

    .btn-success:hover {
      opacity: 0.9;
    }

    .btn-secondary {
      background: var(--bg-elevated);
      color: var(--text-secondary);
      border: 1px solid var(--border);
    }

    .btn-secondary:hover {
      background: var(--bg-surface);
    }

    .create-form {
      background: var(--bg-surface);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 24px;
      margin-bottom: 32px;
      box-shadow: var(--shadow-sm);
    }

    .create-form h2 {
      font-size: 18px;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0 0 20px;
    }

    .form-layout {
      display: flex;
      flex-direction: column;
      gap: 16px;
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
      min-height: 80px;
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
      gap: 10px;
      margin-top: 10px;
    }

    .templates-section {
      margin-top: 32px;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }

    .section-header h2 {
      font-size: 18px;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0;
    }

    .count {
      font-size: 13px;
      color: var(--text-muted);
      background: var(--bg-elevated);
      padding: 4px 12px;
      border-radius: 4px;
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

    .empty-state {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-muted);
      background: var(--bg-surface);
      border-radius: 8px;
      border: 1px solid var(--border);
    }

    .empty-state .btn {
      margin-top: 16px;
    }

    .templates-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
      gap: 16px;
    }

    .template-card {
      background: var(--bg-surface);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 16px;
      transition: all 0.2s;
      box-shadow: var(--shadow-sm);
    }

    .template-card:hover {
      border-color: var(--accent);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .card-header {
      margin-bottom: 12px;
    }

    .card-title {
      display: flex;
      justify-content: space-between;
      align-items: start;
      gap: 10px;
      margin-bottom: 6px;
    }

    .card-title h3 {
      font-size: 15px;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0;
    }

    .badge {
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      white-space: nowrap;
    }

    .badge-public {
      background: var(--accent-muted);
      color: var(--accent);
    }

    .badge-private {
      background: var(--bg-elevated);
      color: var(--text-muted);
    }

    .badge-type {
      background: var(--badge-type-bg);
      color: var(--badge-type-text);
    }

    .owner-badge {
      font-size: 12px;
      color: var(--text-muted);
      font-weight: 500;
    }

    .card-description {
      font-size: 13px;
      color: var(--text-secondary);
      margin: 0 0 12px;
      line-height: 1.4;
    }

    .card-details {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin: 12px 0;
      padding: 12px 0;
      border-top: 1px solid var(--border-subtle);
      border-bottom: 1px solid var(--border-subtle);
    }

    .detail-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
      font-size: 12px;
    }

    .detail-label {
      color: var(--text-muted);
      font-weight: 500;
    }

    .detail-value {
      color: var(--text-secondary);
    }

    .detail-value.mono {
      font-family: 'JetBrains Mono', monospace;
      font-size: 11px;
    }

    .card-actions {
      display: flex;
      gap: 8px;
      margin-top: 12px;
    }

    .action-btn {
      flex: 1;
      padding: 8px 12px;
      border: 1px solid var(--border);
      background: var(--bg-elevated);
      color: var(--text-secondary);
      border-radius: 4px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s;
      text-align: center;
    }

    .action-btn:hover {
      border-color: var(--text-muted);
      background: var(--bg-surface);
      color: var(--text-primary);
    }

    .action-btn.primary {
      background: var(--accent);
      color: var(--accent-text);
      border-color: var(--accent);
    }

    .action-btn.primary:hover {
      background: var(--accent-hover);
      border-color: var(--accent-hover);
    }

    .action-btn.danger {
      color: var(--danger);
      border-color: var(--danger-muted);
    }

    .action-btn.danger:hover {
      background: var(--danger-muted);
      border-color: var(--danger);
      color: var(--danger);
    }

    @media (max-width: 768px) {
      .form-row {
        grid-template-columns: 1fr;
      }

      .templates-grid {
        grid-template-columns: 1fr;
      }

      .page-header {
        flex-direction: column;
        gap: 16px;
      }
    }
  `]
})
export class JobTemplatesComponent implements OnInit {
  templates: JobTemplate[] = [];
  filteredTemplates: JobTemplate[] = [];
  loading = true;
  showCreateForm = false;

  newTemplate: JobTemplateRequest = {
    name: '',
    description: '',
    jobType: '',
    payload: '{}',
    cronExpression: '',
    maxRetries: 3,
    isPublic: false
  };

  constructor(
    private templateService: JobTemplateService,
    private toast: ToastService,
    private router: Router,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    this.templateService.getAvailable().subscribe({
      next: (templates) => {
        this.templates = templates;
        this.filteredTemplates = templates;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load templates:', err);
        this.toast.error('Failed to load templates.');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleCreateForm(): void {
    this.showCreateForm = !this.showCreateForm;
    if (!this.showCreateForm) {
      this.resetForm();
    }
  }

  resetForm(): void {
    this.newTemplate = {
      name: '',
      description: '',
      jobType: '',
      payload: '{}',
      cronExpression: '',
      maxRetries: 3,
      isPublic: false
    };
  }

  createTemplate(): void {
    if (!this.newTemplate.name || !this.newTemplate.jobType) {
      this.toast.warning('Please fill in required fields (Name, Job Type)');
      return;
    }

    this.templateService.create(this.newTemplate).subscribe({
      next: () => {
        this.toast.success('Template created successfully!');
        this.showCreateForm = false;
        this.resetForm();
        this.loadTemplates();
      },
      error: (err) => {
        console.error('Failed to create template:', err);
        this.toast.error('Failed to create template.');
      }
    });
  }

  canEditTemplate(template: JobTemplate): boolean {
    return this.auth.currentUser()?.username === template.ownerName;
  }

  canDeleteTemplate(template: JobTemplate): boolean {
    return this.auth.currentUser()?.username === template.ownerName;
  }

  editTemplate(templateId: number): void {
    this.router.navigate(['/templates', templateId, 'edit']);
  }

  deleteTemplate(templateId: number): void {
    if (!confirm('Are you sure you want to delete this template?')) return;

    this.templateService.delete(templateId).subscribe({
      next: () => {
        this.toast.success('Template deleted successfully.');
        this.loadTemplates();
      },
      error: (err) => {
        console.error('Failed to delete template:', err);
        this.toast.error('Failed to delete template.');
      }
    });
  }

  useTemplate(templateId: number): void {
    this.router.navigate(['/jobs/create'], { queryParams: { template: templateId } });
  }
}

