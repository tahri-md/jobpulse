import { inject, Component } from '@angular/core';
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
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { ChangePasswordRequest } from '../../../models';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="change-password-section">
      <div class="section-header">
        <h2>Change Password</h2>
        <p>Update your password to keep your account secure</p>
      </div>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="change-password-form">
        <div class="form-group">
          <label for="currentPassword">Current Password</label>
          <div class="password-input-wrapper">
            <input
              [type]="showCurrentPassword ? 'text' : 'password'"
              id="currentPassword"
              formControlName="currentPassword"
              placeholder="Enter your current password"
              class="input"
              [class.error]="isFieldInvalid('currentPassword')"
            />
            <button
              type="button"
              class="toggle-password"
              (click)="toggleCurrentPasswordVisibility()"
              tabindex="-1"
            >
              {{ showCurrentPassword ? '●●●' : '○○○' }}
            </button>
          </div>
          @if (isFieldInvalid('currentPassword')) {
            <span class="error-message"> Current password is required </span>
          }
        </div>

        <div class="form-group">
          <label for="newPassword">New Password</label>
          <div class="password-input-wrapper">
            <input
              [type]="showNewPassword ? 'text' : 'password'"
              id="newPassword"
              formControlName="newPassword"
              placeholder="Enter new password"
              class="input"
              [class.error]="isFieldInvalid('newPassword')"
            />
            <button
              type="button"
              class="toggle-password"
              (click)="toggleNewPasswordVisibility()"
              tabindex="-1"
            >
              {{ showNewPassword ? '●●●' : '○○○' }}
            </button>
          </div>
          @if (isFieldInvalid('newPassword')) {
            <span class="error-message"> Password must be at least 8 characters </span>
          }
          <div class="password-requirements">
            <p [class.met]="form.get('newPassword')?.value?.length >= 8">At least 8 characters</p>
          </div>
        </div>

        <div class="form-group">
          <label for="passwordConfirmation">Confirm Password</label>
          <input
            [type]="showNewPassword ? 'text' : 'password'"
            id="passwordConfirmation"
            formControlName="passwordConfirmation"
            placeholder="Confirm new password"
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

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" [disabled]="!form.valid || isLoading">
            @if (!isLoading) {
              <span>Change Password</span>
            }
            @if (isLoading) {
              <span>Changing...</span>
            }
          </button>
          <button type="button" (click)="resetForm()" class="btn btn-secondary">Cancel</button>
        </div>
      </form>
    </div>
  `,
  styles: [
    `
      .change-password-section {
        background: var(--bg-surface);
        border: 1px solid var(--border);
        border-radius: 8px;
        padding: 24px;
      }

      .section-header {
        margin-bottom: 24px;
      }

      .section-header h2 {
        font-size: 18px;
        font-weight: 700;
        color: var(--text-primary);
        margin: 0 0 4px;
      }

      .section-header p {
        font-size: 13px;
        color: var(--text-muted);
        margin: 0;
      }

      .change-password-form {
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

      .form-actions {
        display: flex;
        gap: 12px;
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
        text-align: center;
        font-family: inherit;
        flex: 1;
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
      }

      .btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }
    `,
  ],
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toast = inject(ToastService);

  form: FormGroup;
  isLoading = false;
  showCurrentPassword = false;
  showNewPassword = false;

  constructor() {
    this.form = this.fb.group(
      {
        currentPassword: ['', Validators.required],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        passwordConfirmation: ['', Validators.required],
      },
      { validators: this.passwordMatchValidator },
    );
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    this.isLoading = true;
    const request: ChangePasswordRequest = {
      currentPassword: this.form.get('currentPassword')?.value,
      newPassword: this.form.get('newPassword')?.value,
      passwordConfirmation: this.form.get('passwordConfirmation')?.value,
    };

    this.authService.changePassword(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.toast.success('Password changed successfully!');
        this.resetForm();
      },
      error: (err) => {
        this.isLoading = false;
        const message = err.error?.message || 'Failed to change password. Please try again.';
        this.toast.error(message);
      },
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.form.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  toggleCurrentPasswordVisibility(): void {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  toggleNewPasswordVisibility(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  resetForm(): void {
    this.isLoading = false;
    this.form.reset();
    this.showCurrentPassword = false;
    this.showNewPassword = false;
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const passwordConfirmation = control.get('passwordConfirmation')?.value;

    if (!newPassword || !passwordConfirmation) {
      return null;
    }

    return newPassword === passwordConfirmation ? null : { passwordMismatch: true };
  }
}
