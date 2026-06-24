import { inject, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ResetPasswordRequest } from '../../models';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-header">
          <span class="auth-logo">//</span>
          <h1>Create New Password</h1>
          <p>Enter your new password below</p>
        </div>

        @if (!isInvalidToken && !resetSuccess) {
          <div class="form-container">
            <form [formGroup]="form" (ngSubmit)="onSubmit()">
              <div class="form-group">
                <label for="password">New Password</label>
                <div class="password-input-wrapper">
                  <input
                    [type]="showPassword ? 'text' : 'password'"
                    id="password"
                    formControlName="password"
                    placeholder="Enter new password"
                    class="input"
                    [class.error]="isFieldInvalid('password')"
                  />
                  <button
                    type="button"
                    class="toggle-password"
                    (click)="togglePasswordVisibility()"
                    tabindex="-1"
                  >
                    {{ showPassword ? '●●●' : '○○○' }}
                  </button>
                </div>
                @if (isFieldInvalid('password')) {
                  <span class="error-message"> Password must be at least 8 characters </span>
                }
                <div class="password-requirements">
                  <p [class.met]="form.get('password')?.value?.length >= 8">
                    At least 8 characters
                  </p>
                </div>
              </div>

              <div class="form-group">
                <label for="passwordConfirmation">Confirm Password</label>
                <input
                  [type]="showPassword ? 'text' : 'password'"
                  id="passwordConfirmation"
                  formControlName="passwordConfirmation"
                  placeholder="Confirm password"
                  class="input"
                  [class.error]="isFieldInvalid('passwordConfirmation')"
                />
                @if (isFieldInvalid('passwordConfirmation')) {
                  <span class="error-message">
                    {{
                      form.hasError('passwordMismatch')
                        ? 'Passwords do not match'
                        : 'Please confirm your password'
                    }}
                  </span>
                }
              </div>

              <button type="submit" class="btn btn-primary" [disabled]="!form.valid || isLoading">
                @if (!isLoading) {
                  <span>Reset Password</span>
                }
                @if (isLoading) {
                  <span>Resetting...</span>
                }
              </button>
            </form>

            <div class="divider"></div>

            <div class="auth-links">
              <p>Remember your password? <a routerLink="/login">Back to Login</a></p>
            </div>
          </div>
        }

        @if (isInvalidToken) {
          <div class="error-container">
            <div class="error-icon">✕</div>
            <p class="status-message">Invalid Reset Link</p>
            <p class="status-description">
              The password reset link is invalid or has expired. Please request a new one.
            </p>
            <button (click)="goToForgotPassword()" class="btn btn-primary">Request New Link</button>
            <a routerLink="/login" class="btn btn-secondary"> Back to Login </a>
          </div>
        }

        @if (resetSuccess) {
          <div class="success-container">
            <div class="success-icon">✓</div>
            <p class="status-message">Password Reset Successful</p>
            <p class="status-description">
              Your password has been reset. You can now log in with your new password.
            </p>
            <button (click)="goToLogin()" class="btn btn-primary">Go to Login</button>
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

      .password-input-wrapper {
        position: relative;
        display: flex;
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
        flex: 1;
      }

      .input:focus {
        outline: none;
        border-color: var(--accent);
        box-shadow: 0 0 0 3px var(--accent-faint);
      }

      .input.error {
        border-color: #ef4444;
      }

      .toggle-password {
        position: absolute;
        right: 12px;
        top: 50%;
        transform: translateY(-50%);
        background: none;
        border: none;
        cursor: pointer;
        color: var(--text-muted);
        font-size: 13px;
        font-weight: 600;
        transition: color 0.15s;
      }

      .toggle-password:hover {
        color: var(--text-primary);
      }

      .error-message {
        font-size: 12px;
        color: #ef4444;
      }

      .password-requirements {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }

      .password-requirements p {
        font-size: 12px;
        color: var(--text-muted);
        margin: 0;
        display: flex;
        align-items: center;
        gap: 6px;
      }

      .password-requirements p.met {
        color: #10b981;
      }

      .password-requirements p::before {
        content: '○';
        display: inline-block;
        width: 14px;
        text-align: center;
      }

      .password-requirements p.met::before {
        content: '✓';
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

      .btn-secondary {
        background: var(--bg-elevated);
        color: var(--text-secondary);
        border: 1px solid var(--border);
        margin-top: 8px;
      }

      .btn-secondary:hover {
        background: var(--bg-hover);
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

      .error-container,
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
        margin: 0;
      }

      .status-description {
        font-size: 13px;
        color: var(--text-muted);
        margin: 0;
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
export class ResetPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);

  form: FormGroup;
  isLoading = false;
  resetSuccess = false;
  isInvalidToken = false;
  showPassword = false;
  private resetToken = '';

  constructor() {
    this.form = this.fb.group(
      {
        password: ['', [Validators.required, Validators.minLength(8)]],
        passwordConfirmation: ['', Validators.required],
      },
      { validators: this.passwordMatchValidator },
    );
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.resetToken = params['token'];
      if (!this.resetToken) {
        this.isInvalidToken = true;
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid || !this.resetToken) {
      return;
    }

    this.isLoading = true;
    const request: ResetPasswordRequest = {
      token: this.resetToken,
      newPassword: this.form.get('password')?.value,
      passwordConfirmation: this.form.get('passwordConfirmation')?.value,
    };

    this.authService.resetPassword(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.resetSuccess = true;
        this.toast.success('Password reset successfully!');
      },
      error: (err) => {
        this.isLoading = false;
        const message = err.error?.message || 'Failed to reset password. Please try again.';
        this.toast.error(message);
      },
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToForgotPassword(): void {
    this.router.navigate(['/forgot-password']);
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const passwordConfirmation = control.get('passwordConfirmation')?.value;

    if (!password || !passwordConfirmation) {
      return null;
    }

    return password === passwordConfirmation ? null : { passwordMismatch: true };
  }
}
