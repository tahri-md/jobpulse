import { inject, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { ForgotPasswordRequest } from '../../models';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-header">
          <span class="auth-logo">//</span>
          <h1>Reset Password</h1>
          <p>Enter your email to receive password reset instructions</p>
        </div>

        @if (!emailSent) {
          <div class="form-container">
            <form [formGroup]="form" (ngSubmit)="onSubmit()">
              <div class="form-group">
                <label for="email">Email Address</label>
                <input
                  type="email"
                  id="email"
                  formControlName="email"
                  placeholder="Enter your email"
                  class="input"
                  [class.error]="isFieldInvalid('email')"
                />
                @if (isFieldInvalid('email')) {
                  <span class="error-message"> Please enter a valid email address </span>
                }
              </div>

              <button type="submit" class="btn btn-primary" [disabled]="!form.valid || isLoading">
                @if (!isLoading) {
                  <span>Send Reset Link</span>
                }
                @if (isLoading) {
                  <span>Sending...</span>
                }
              </button>
            </form>

            <div class="divider"></div>

            <div class="auth-links">
              <p>Remember your password? <a routerLink="/login">Back to Login</a></p>
              <p>Don't have an account? <a routerLink="/register">Create one</a></p>
            </div>
          </div>
        }

        @if (emailSent) {
          <div class="success-container">
            <div class="success-icon">✓</div>
            <p class="status-message">Check Your Email</p>
            <p class="status-description">
              We've sent password reset instructions to
              <strong>{{ form.get('email')?.value }}</strong>
            </p>
            <p class="status-hint">The link will expire in 24 hours.</p>
            <button (click)="goBack()" class="btn btn-primary">Back to Login</button>
          </div>
        }

        <div class="auth-footer">
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

      .form-container {
        display: flex;
        flex-direction: column;
        gap: 24px;
      }

      form {
        display: flex;
        flex-direction: column;
        gap: 20px;
      }

      .form-group {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      label {
        font-size: 12px;
        font-weight: 600;
        color: var(--text-secondary);
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .input {
        padding: 11px 12px;
        border: 1px solid var(--border);
        border-radius: 6px;
        font-size: 13px;
        color: var(--text-primary);
        background: var(--bg-input);
        transition: all 0.15s;
        font-family: inherit;
      }

      .input:focus {
        outline: none;
        border-color: var(--accent);
        box-shadow: 0 0 0 3px var(--accent-faint);
      }

      .input.error {
        border-color: #ef4444;
      }

      .error-message {
        font-size: 12px;
        color: #ef4444;
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
        font-family: inherit;
      }

      .btn-primary {
        background: var(--accent);
        color: var(--accent-text);
      }

      .btn-primary:hover:not(:disabled) {
        background: var(--accent-hover);
      }

      .btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .divider {
        height: 1px;
        background: var(--border);
      }

      .auth-links {
        display: flex;
        flex-direction: column;
        gap: 12px;
        text-align: center;
      }

      .auth-links p {
        font-size: 13px;
        color: var(--text-muted);
        margin: 0;
      }

      .auth-links a {
        color: var(--accent);
        text-decoration: none;
        font-weight: 600;
        transition: opacity 0.15s;
      }

      .auth-links a:hover {
        opacity: 0.8;
      }

      .success-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16px;
        text-align: center;
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

      .status-message {
        font-size: 18px;
        font-weight: 600;
        color: var(--text-primary);
        margin: 0;
      }

      .status-description {
        font-size: 13px;
        color: var(--text-muted);
        margin: 0;
      }

      .status-hint {
        font-size: 12px;
        color: var(--text-muted);
        margin: 0;
        font-style: italic;
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
    `,
  ],
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  form: FormGroup;
  isLoading = false;
  emailSent = false;

  constructor() {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    this.isLoading = true;
    const request: ForgotPasswordRequest = { email: this.form.get('email')?.value };

    this.authService.forgotPassword(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.emailSent = true;
        this.toast.success('Password reset email sent successfully!');
      },
      error: (err) => {
        this.isLoading = false;
        const message = err.error?.message || 'Failed to send reset email. Please try again.';
        this.toast.error(message);
      },
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  goBack(): void {
    this.router.navigate(['/login']);
  }
}
