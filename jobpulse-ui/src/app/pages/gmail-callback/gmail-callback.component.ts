import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { GmailService } from '../../services/gmail.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-gmail-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback-page">
      <div class="callback-card">
        @if (processing) {
          <div class="spinner"></div>
          <p>Connecting your Gmail account...</p>
        } @else if (error) {
          <p class="error-text">{{ error }}</p>
          <button class="btn" (click)="goToProfile()">Back to Profile</button>
        } @else {
          <p class="success-text">Gmail connected successfully!</p>
          <p class="detail">Redirecting...</p>
        }
      </div>
    </div>
  `,
  styles: [`
    .callback-page {
      min-height: 100vh; display: flex; align-items: center; justify-content: center;
      background: var(--bg-auth);
    }
    .callback-card {
      background: var(--bg-surface); border-radius: 10px; padding: 48px; text-align: center;
      border: 1px solid var(--border); max-width: 400px; width: 100%; box-shadow: var(--shadow-md);
    }
    .callback-card p { color: var(--text-muted); font-size: 14px; margin: 12px 0 0; }
    .spinner {
      width: 32px; height: 32px; border: 2px solid var(--border); border-top-color: var(--accent);
      border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .error-text { color: var(--danger) !important; font-weight: 600; }
    .success-text { color: var(--accent) !important; font-weight: 600; font-size: 16px !important; }
    .detail { color: var(--text-muted) !important; }
    .btn {
      margin-top: 20px; padding: 9px 24px; background: var(--bg-elevated); color: var(--text-secondary); border: 1px solid var(--border-hover);
      border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.15s;
    }
    .btn:hover { background: var(--bg-hover); }
  `]
})
export class GmailCallbackComponent implements OnInit {
  processing = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private gmailService: GmailService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const code = this.route.snapshot.queryParamMap.get('code');
    const errorParam = this.route.snapshot.queryParamMap.get('error');

    if (errorParam) {
      this.processing = false;
      this.error = 'Google authorization was cancelled or failed.';
      return;
    }

    if (!code) {
      this.processing = false;
      this.error = 'No authorization code received.';
      return;
    }

    this.gmailService.exchangeCode(code).subscribe({
      next: (status) => {
        this.processing = false;
        this.toast.success(`Gmail connected: ${status.gmailAddress}`);
        setTimeout(() => this.router.navigate(['/profile']), 1500);
      },
      error: (err) => {
        this.processing = false;
        this.error = err.error?.message || 'Failed to connect Gmail account.';
      }
    });
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }
}
