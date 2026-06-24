import { inject, Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-header">
          <span class="auth-logo">//</span>
          <h1>Verify Email</h1>
          <p>Confirming your email address</p>
        </div>

        <!-- Loading State -->
        <div @if (verificationState="" ="" ="loading" ) { class="verification-content">
          <div class="spinner"></div>
          <p class="status-message">Verifying your email...</p>
        </div>

        <!-- Success State -->
        <div @if (verificationState="" ="" ="success" ) { class="verification-content success">
          <div class="success-icon">✓</div>
          <p class="status-message">Email verified successfully!</p>
          <p class="status-description">
            Your account is now active. You can log in with your credentials.
          </p>
          <button (click)="redirectToLogin()" class="btn btn-primary">Go to Login</button>
        </div>

        <!-- Error State -->
        <div @if (verificationState="" ="" ="error" ) { class="verification-content error">
          <div class="error-icon">✕</div>
          <p class="status-message">Verification Failed</p>
          <p class="status-description">{{ errorMessage }}</p>
          <button (click)="requestNewLink()" class="btn btn-primary">Request New Link</button>
          <a routerLink="/login" class="btn btn-secondary"> Back to Login </a>
        </div>

        <!-- Invalid Token State -->
        <div @if (verificationState="" ="" ="invalid" ) { class="verification-content error">
          <div class="error-icon">!</div>
          <p class="status-message">Invalid Verification Link</p>
          <p class="status-description">The verification link is missing or has expired.</p>
          <button (click)="requestNewLink()" class="btn btn-primary">Request New Link</button>
          <a routerLink="/register" class="btn btn-secondary"> Create New Account </a>
        </div>

        <div class="auth-footer" @if (verificationState !="" ="success" ) {>
          <p>Need help? <a href="mailto:support@jobpulse.com">Contact support</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .auth-page {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--bg-auth);
        padding: 20px;
      }
      .auth-card {
        background: var(--bg-surface);
        border-radius: 10px;
        padding: 40px;
        width: 100%;
        max-width: 400px;
        border: 1px solid var(--border);
        box-shadow: var(--shadow-md);
      }
      .auth-header {
        text-align: center;
        margin-bottom: 32px;
      }
      .auth-logo {
        font-size: 32px;
        font-weight: 900;
        color: var(--accent);
        display: block;
        margin-bottom: 16px;
        font-family: monospace;
      }
      .auth-header h1 {
        font-size: 22px;
        font-weight: 700;
        color: var(--text-primary);
        margin: 0 0 8px;
      }
      .auth-header p {
        font-size: 13px;
        color: var(--text-muted);
        margin: 0;
      }

      .verification-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16px;
      }

      .spinner {
        width: 48px;
        height: 48px;
        border: 3px solid var(--border);
        border-top-color: var(--accent);
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }
      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }

      .success-icon {
        width: 64px;
        height: 64px;
        background: #10b981;
        color: white;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32px;
        font-weight: 700;
      }
      .error-icon {
        width: 64px;
        height: 64px;
        background: #ef4444;
        color: white;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32px;
        font-weight: 700;
      }

      .status-message {
        font-size: 18px;
        font-weight: 600;
        color: var(--text-primary);
        margin: 8px 0 0;
        text-align: center;
      }
      .status-description {
        font-size: 13px;
        color: var(--text-muted);
        text-align: center;
        margin: 0;
      }

      .btn {
        padding: 11px 16px;
        border: none;
        border-radius: 6px;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.15s;
        text-decoration: none;
        display: inline-block;
        text-align: center;
        width: 100%;
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
      .btn-secondary:hover {
        background: var(--bg-hover);
        border-color: var(--border-hover);
      }
      .btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .auth-footer {
        text-align: center;
        margin-top: 24px;
        font-size: 13px;
        color: var(--text-muted);
      }
      .auth-footer a {
        color: var(--accent);
        text-decoration: none;
        font-weight: 600;
      }
      .auth-footer a:hover {
        text-decoration: underline;
      }

      .verification-content.success {
        gap: 20px;
      }
      .verification-content.error {
        gap: 20px;
      }
    `,
  ],
})
export class VerifyEmailComponent implements OnInit {
  verificationState: 'loading' | 'success' | 'error' | 'invalid' = 'loading';
  errorMessage = 'The verification link is invalid or has expired. Please request a new one.';

  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.verifyEmail();
  }

  verifyEmail(): void {
    this.route.queryParams.subscribe((params) => {
      const token = params['token'];

      if (!token) {
        this.verificationState = 'invalid';
        this.cdr.markForCheck();
        return;
      }

      this.authService.verifyEmail(token).subscribe({
        next: () => {
          this.verificationState = 'success';
          this.cdr.markForCheck();
          this.toast.success('Email verified successfully!');
        },
        error: (err) => {
          this.verificationState = 'error';
          this.cdr.markForCheck();
          this.errorMessage =
            err.error?.message ||
            'The verification link is invalid or has expired. Please request a new one.';
          this.toast.error(this.errorMessage);
        },
      });
    });
  }

  redirectToLogin(): void {
    this.router.navigate(['/login']);
  }

  requestNewLink(): void {
    this.router.navigate(['/resend-verification']);
  }
}
