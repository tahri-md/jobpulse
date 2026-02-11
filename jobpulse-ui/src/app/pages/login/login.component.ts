import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { OAuthService } from '../../services/oauth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-header">
          <span class="auth-logo">//</span>
          <h1>Welcome Back</h1>
          <p>Sign in to your JobPulse account</p>
        </div>

        <!-- OAuth Buttons -->
        <div class="oauth-buttons">
          <button class="btn btn-oauth btn-google" (click)="onGoogleLogin()" [disabled]="oauthLoading">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path fill="#fff" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
              <path fill="#fff" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#fff" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#fff" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            <span>{{ oauthLoading === 'google' ? 'Connecting...' : 'Continue with Google' }}</span>
          </button>
          <button class="btn btn-oauth btn-github" (click)="onGitHubLogin()" [disabled]="oauthLoading">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path fill="#fff" d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
            </svg>
            <span>{{ oauthLoading === 'github' ? 'Connecting...' : 'Continue with GitHub' }}</span>
          </button>
        </div>

        <div class="divider">
          <span>or</span>
        </div>

        <form (ngSubmit)="onLogin()" class="auth-form">
          <div class="form-group">
            <label for="username">Username</label>
            <input id="username" type="text" [(ngModel)]="username" name="username" placeholder="Enter your username" required autocomplete="username" />
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input id="password" type="password" [(ngModel)]="password" name="password" placeholder="Enter your password" required autocomplete="current-password" />
          </div>

          <button type="submit" class="btn btn-primary" [disabled]="loading">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <div class="auth-footer">
          <p>Don't have an account? <a routerLink="/register">Sign up</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      min-height: 100vh; display: flex; align-items: center; justify-content: center;
      background: var(--bg-auth); padding: 20px;
    }
    .auth-card {
      background: var(--bg-surface); border-radius: 10px; padding: 40px; width: 100%;
      max-width: 400px; border: 1px solid var(--border); box-shadow: var(--shadow-md);
    }
    .auth-header { text-align: center; margin-bottom: 32px; }
    .auth-logo {
      font-size: 32px; font-weight: 900; color: var(--accent); display: block;
      margin-bottom: 16px; font-family: monospace;
    }
    .auth-header h1 { font-size: 22px; font-weight: 700; color: var(--text-primary); margin: 0 0 8px; }
    .auth-header p { font-size: 13px; color: var(--text-muted); margin: 0; }

    /* OAuth Buttons */
    .oauth-buttons { display: flex; flex-direction: column; gap: 10px; }
    .btn-oauth {
      display: flex; align-items: center; justify-content: center; gap: 10px;
      padding: 11px; border: 1px solid var(--border); border-radius: 6px;
      font-size: 13px; font-weight: 600; cursor: pointer;
      transition: all 0.15s; background: var(--bg-elevated); color: var(--text-secondary);
    }
    .btn-oauth:hover:not(:disabled) { background: var(--bg-hover); border-color: var(--border-hover); }
    .btn-oauth:disabled { opacity: 0.4; cursor: not-allowed; }
    .btn-oauth svg { flex-shrink: 0; }

    /* Divider */
    .divider {
      display: flex; align-items: center; gap: 12px;
      margin: 22px 0;
    }
    .divider::before, .divider::after {
      content: ''; flex: 1; height: 1px; background: var(--border);
    }
    .divider span { font-size: 12px; color: var(--text-faint); text-transform: uppercase; letter-spacing: 1px; }

    .auth-form { display: flex; flex-direction: column; gap: 18px; }
    .form-group { display: flex; flex-direction: column; gap: 6px; }
    .form-group label { font-size: 13px; font-weight: 600; color: var(--text-muted); }
    .form-group input {
      padding: 10px 14px; border: 1px solid var(--border); border-radius: 6px;
      font-size: 14px; outline: none; transition: border-color 0.15s;
      background: var(--bg-input); color: var(--text-input);
    }
    .form-group input::placeholder { color: var(--text-placeholder); }
    .form-group input:focus { border-color: var(--accent); }
    .btn {
      padding: 11px; border: none; border-radius: 6px; font-size: 13px;
      font-weight: 600; cursor: pointer; transition: all 0.15s;
    }
    .btn-primary { background: var(--accent); color: var(--accent-text); }
    .btn-primary:hover:not(:disabled) { background: var(--accent-hover); }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .auth-footer { text-align: center; margin-top: 24px; font-size: 13px; color: var(--text-muted); }
    .auth-footer a { color: var(--accent); text-decoration: none; font-weight: 600; }
    .auth-footer a:hover { text-decoration: underline; }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  oauthLoading: string | null = null;

  constructor(
    private authService: AuthService,
    private oauthService: OAuthService,
    private router: Router,
    private toast: ToastService
  ) {}

  onLogin(): void {
    if (!this.username || !this.password) {
      this.toast.warning('Please fill in all fields.');
      return;
    }

    this.loading = true;
    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: () => {
        this.toast.success('Welcome back!');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        const message = err.error?.message || 'Invalid credentials.';
        this.toast.error(message);
      }
    });
  }

  onGoogleLogin(): void {
    this.oauthLoading = 'google';
    this.oauthService.signInWithGoogle();
  }

  onGitHubLogin(): void {
    this.oauthLoading = 'github';
    this.oauthService.signInWithGitHub();
  }
}
