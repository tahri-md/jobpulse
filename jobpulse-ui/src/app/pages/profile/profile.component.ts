import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { GmailService, GmailStatus } from '../../services/gmail.service';
import { ToastService } from '../../services/toast.service';
import { UserResponse } from '../../models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Profile</h1>
        <p class="subtitle">Your account information</p>
      </div>

      @if (user) {
        <div class="profile-card">
          <div class="avatar-section">
            <div class="avatar">{{ user.username.charAt(0).toUpperCase() }}</div>
            <h2>{{ user.username }}</h2>
            <span class="role-badge">{{ user.role }}</span>
          </div>

          <div class="info-section">
            <div class="info-row">
              <span class="info-label">Username</span>
              <span class="info-value">{{ user.username }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Email</span>
              <span class="info-value">{{ user.email || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Role</span>
              <span class="info-value">{{ user.role }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">User ID</span>
              <span class="info-value">#{{ user.id }}</span>
            </div>
          </div>
        </div>

        <!-- Gmail Integration -->
        <div class="section-card">
          <h3>Gmail Integration</h3>
          <p class="section-desc">Connect your Gmail to send emails from your account when jobs run.</p>

          @if (gmailLoading) {
            <div class="gmail-status loading">
              <span class="status-dot neutral"></span>
              <span>Checking connection...</span>
            </div>
          } @else if (gmailStatus?.connected) {
            <div class="gmail-status connected">
              <span class="status-dot active"></span>
              <div class="gmail-info">
                <span class="gmail-label">Connected</span>
                <span class="gmail-address">{{ gmailStatus?.gmailAddress }}</span>
              </div>
              <button class="btn btn-danger-outline" (click)="disconnectGmail()" [disabled]="gmailActing">
                {{ gmailActing ? 'Disconnecting...' : 'Disconnect' }}
              </button>
            </div>
          } @else {
            <div class="gmail-status disconnected">
              <span class="status-dot"></span>
              <span>Not connected</span>
              <button class="btn btn-accent" (click)="connectGmail()" [disabled]="gmailActing">
                {{ gmailActing ? 'Redirecting...' : 'Connect Gmail' }}
              </button>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .page { max-width: 600px; }
    .page-header { margin-bottom: 28px; }
    .page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); margin: 0 0 4px; letter-spacing: -0.5px; }
    .subtitle { font-size: 14px; color: var(--text-muted); margin: 0; }
    .profile-card {
      background: var(--bg-surface); border-radius: 8px; overflow: hidden; border: 1px solid var(--border);
      box-shadow: var(--shadow-sm);
    }
    .avatar-section {
      text-align: center; padding: 40px 20px 30px;
      background: var(--bg-input); border-bottom: 1px solid var(--border); color: var(--text-primary);
    }
    .avatar {
      width: 72px; height: 72px; border-radius: 6px;
      background: var(--accent); color: var(--accent-text);
      display: flex; align-items: center; justify-content: center;
      font-size: 28px; font-weight: 700; margin: 0 auto 16px;
    }
    .avatar-section h2 { font-size: 20px; margin: 0 0 8px; color: var(--text-primary); }
    .role-badge {
      display: inline-block; padding: 4px 14px;
      background: var(--badge-type-bg); border: 1px solid var(--border-hover); border-radius: 4px;
      font-size: 11px; font-weight: 600; color: var(--text-muted);
      text-transform: uppercase; letter-spacing: 1px;
    }
    .info-section { padding: 24px; }
    .info-row {
      display: flex; justify-content: space-between;
      padding: 14px 0; border-bottom: 1px solid var(--border);
    }
    .info-row:last-child { border: none; }
    .info-label { font-size: 13px; color: var(--text-muted); font-weight: 500; }
    .info-value { font-size: 13px; color: var(--text-secondary); font-weight: 600; }

    /* Gmail section */
    .section-card {
      background: var(--bg-surface); border-radius: 8px; padding: 24px;
      border: 1px solid var(--border); margin-top: 20px; box-shadow: var(--shadow-sm);
    }
    .section-card h3 {
      font-size: 14px; font-weight: 600; color: var(--text-primary); margin: 0 0 6px;
    }
    .section-desc { font-size: 13px; color: var(--text-muted); margin: 0 0 20px; }
    .gmail-status {
      display: flex; align-items: center; gap: 12px;
      padding: 14px 16px; border-radius: 6px; background: var(--bg-input); border: 1px solid var(--border);
    }
    .status-dot {
      width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
      background: var(--border-hover);
    }
    .status-dot.active { background: var(--success); }
    .status-dot.neutral { background: var(--text-muted); }
    .gmail-info { display: flex; flex-direction: column; flex: 1; }
    .gmail-label { font-size: 12px; color: var(--success); font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .gmail-address { font-size: 14px; color: var(--text-secondary); font-weight: 500; margin-top: 2px; }
    .gmail-status.disconnected span:not(.status-dot) { font-size: 13px; color: var(--text-muted); flex: 1; }
    .gmail-status.loading span:not(.status-dot) { font-size: 13px; color: var(--text-muted); }
    .btn {
      padding: 8px 18px; border: none; border-radius: 6px; font-size: 12px;
      font-weight: 600; cursor: pointer; transition: all 0.15s; white-space: nowrap;
    }
    .btn-accent { background: var(--accent); color: var(--accent-text); }
    .btn-accent:hover:not(:disabled) { background: var(--accent-hover); }
    .btn-danger-outline {
      background: none; color: var(--danger); border: 1px solid var(--danger-border);
    }
    .btn-danger-outline:hover:not(:disabled) { background: var(--danger-muted); border-color: var(--danger); }
    .btn:disabled { opacity: 0.4; cursor: not-allowed; }
  `]
})
export class ProfileComponent implements OnInit {
  user: UserResponse | null = null;
  gmailStatus: GmailStatus | null = null;
  gmailLoading = true;
  gmailActing = false;

  constructor(
    private authService: AuthService,
    private gmailService: GmailService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.user = this.authService.currentUser();
    this.authService.getMe().subscribe({
      next: (u) => this.user = u
    });
    this.loadGmailStatus();
  }

  loadGmailStatus(): void {
    this.gmailLoading = true;
    this.gmailService.getStatus().subscribe({
      next: (status) => {
        this.gmailStatus = status;
        this.gmailLoading = false;
      },
      error: () => {
        this.gmailLoading = false;
      }
    });
  }

  connectGmail(): void {
    this.gmailActing = true;
    this.gmailService.getAuthUrl().subscribe({
      next: (res) => {
        // Redirect to Google's consent screen
        window.location.href = res.url;
      },
      error: () => {
        this.gmailActing = false;
        this.toast.error('Failed to start Gmail connection.');
      }
    });
  }

  disconnectGmail(): void {
    if (!confirm('Disconnect your Gmail account? Email jobs will fall back to SMTP.')) return;
    this.gmailActing = true;
    this.gmailService.disconnect().subscribe({
      next: () => {
        this.gmailStatus = { gmailAddress: null, connected: false };
        this.gmailActing = false;
        this.toast.success('Gmail disconnected.');
      },
      error: () => {
        this.gmailActing = false;
        this.toast.error('Failed to disconnect Gmail.');
      }
    });
  }
}
