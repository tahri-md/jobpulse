import { Component, signal, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <aside class="sidebar" [class.collapsed]="collapsed()">
      <div class="sidebar-header">
        <div class="logo">
          <span class="logo-mark">//</span>
          <span class="logo-text">JobPulse</span>
        </div>
        <button class="toggle-btn" (click)="toggleSidebar()" [title]="collapsed() ? 'Expand sidebar' : 'Collapse sidebar'">
          <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
            @if (collapsed()) {
              <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
            } @else {
              <path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd"/>
            }
          </svg>
        </button>
      </div>

      <nav class="sidebar-nav">
        <a routerLink="/dashboard" routerLinkActive="active" class="nav-item" [title]="collapsed() ? 'Dashboard' : ''">
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><rect x="2" y="2" width="7" height="7" rx="1"/><rect x="11" y="2" width="7" height="7" rx="1"/><rect x="2" y="11" width="7" height="7" rx="1"/><rect x="11" y="11" width="7" height="7" rx="1"/></svg>
          <span class="nav-label">Dashboard</span>
        </a>
        <a routerLink="/jobs" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" class="nav-item" [title]="collapsed() ? 'Jobs' : ''">
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 4h14v2H3zm0 5h14v2H3zm0 5h10v2H3z"/></svg>
          <span class="nav-label">Jobs</span>
        </a>
        <a routerLink="/jobs/create" routerLinkActive="active" class="nav-item" [title]="collapsed() ? 'Create Job' : ''">
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10 3v14M3 10h14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/></svg>
          <span class="nav-label">Create Job</span>
        </a>
        <a routerLink="/dead-letter" routerLinkActive="active" class="nav-item" [title]="collapsed() ? 'Dead Letter Queue' : ''">
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10 2L2 7v6l8 5 8-5V7L10 2zm0 3l5 3-5 3-5-3 5-3z"/></svg>
          <span class="nav-label">Dead Letter Queue</span>
        </a>

        <div class="nav-divider"></div>

        <div class="nav-section-title">Account</div>
        <a routerLink="/profile" routerLinkActive="active" class="nav-item" [title]="collapsed() ? 'Profile' : ''">
          <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><circle cx="10" cy="7" r="4"/><path d="M3 17c0-3.3 2.7-6 7-6s7 2.7 7 6"/></svg>
          <span class="nav-label">Profile</span>
        </a>
      </nav>

      <div class="sidebar-footer">
        <div class="footer-top">
          <button class="theme-toggle" (click)="themeService.toggle()" [title]="themeService.theme() === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'">
            @if (themeService.theme() === 'dark') {
              <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z"/></svg>
            } @else {
              <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"/></svg>
            }
          </button>
        </div>
        <div class="footer-bottom">
          <div class="user-info">
            <div class="user-avatar">{{ userInitial() }}</div>
            <div class="user-details">
              <span class="user-name">{{ auth.currentUser()?.username }}</span>
              <span class="user-role">{{ auth.currentUser()?.role }}</span>
            </div>
          </div>
          <button class="logout-btn" (click)="auth.logout()" title="Logout">
            <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18"><path d="M3 3h8v2H5v10h6v2H3V3zm10 4l4 3-4 3V7zm-3 2h6v2h-6v-2z"/></svg>
          </button>
        </div>
      </div>
    </aside>
  `,
  styles: [`
    .sidebar {
      width: 260px;
      height: 100vh;
      background: var(--bg-sidebar);
      color: var(--text-muted);
      display: flex;
      flex-direction: column;
      position: fixed;
      left: 0;
      top: 0;
      z-index: 100;
      border-right: 1px solid var(--border);
      transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      overflow: hidden;
    }
    .sidebar.collapsed { width: 64px; }

    .sidebar-header {
      padding: 18px 16px;
      border-bottom: 1px solid var(--border);
      display: flex;
      align-items: center;
      justify-content: space-between;
      min-height: 64px;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      overflow: hidden;
      white-space: nowrap;
    }
    .logo-mark {
      font-size: 24px;
      font-weight: 700;
      color: var(--accent);
      font-family: 'JetBrains Mono', monospace;
      flex-shrink: 0;
    }
    .logo-text {
      font-size: 20px;
      font-weight: 700;
      color: var(--text-primary);
      letter-spacing: -0.5px;
      opacity: 1;
      transition: opacity 0.2s;
    }
    .collapsed .logo-text { opacity: 0; width: 0; }

    .toggle-btn {
      background: var(--bg-elevated);
      border: 1px solid var(--border);
      cursor: pointer;
      color: var(--text-muted);
      padding: 5px;
      border-radius: 6px;
      transition: all 0.15s;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .toggle-btn:hover { color: var(--accent); border-color: var(--border-hover); background: var(--bg-hover); }
    .collapsed .toggle-btn {
      position: absolute;
      right: 50%;
      transform: translateX(50%);
    }
    .collapsed .sidebar-header { justify-content: center; padding: 18px 8px; }
    .collapsed .logo { display: none; }

    .sidebar-nav {
      flex: 1;
      padding: 16px 12px;
      display: flex;
      flex-direction: column;
      gap: 2px;
      overflow-y: auto;
      overflow-x: hidden;
    }
    .collapsed .sidebar-nav { padding: 16px 8px; }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 14px;
      border-radius: 6px;
      color: var(--text-muted);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.15s;
      white-space: nowrap;
      overflow: hidden;
    }
    .collapsed .nav-item {
      padding: 10px;
      justify-content: center;
    }
    .nav-item:hover {
      background: var(--bg-hover);
      color: var(--text-secondary);
    }
    .nav-item.active {
      background: var(--bg-hover);
      color: var(--accent);
    }
    .nav-icon { width: 18px; height: 18px; flex-shrink: 0; }
    .nav-label {
      opacity: 1;
      transition: opacity 0.2s;
    }
    .collapsed .nav-label { opacity: 0; width: 0; overflow: hidden; }

    .nav-divider {
      height: 1px;
      background: var(--border);
      margin: 12px 0;
    }
    .nav-section-title {
      font-size: 10px;
      text-transform: uppercase;
      letter-spacing: 1.5px;
      color: var(--text-faint);
      padding: 8px 14px 4px;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      opacity: 1;
      transition: opacity 0.2s;
    }
    .collapsed .nav-section-title { opacity: 0; height: 0; padding: 0; margin: 0; }

    .sidebar-footer {
      border-top: 1px solid var(--border);
    }
    .footer-top {
      display: flex;
      justify-content: flex-end;
      padding: 10px 16px 0;
    }
    .collapsed .footer-top { justify-content: center; padding: 10px 8px 0; }

    .footer-bottom {
      padding: 10px 16px 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      overflow: hidden;
    }
    .collapsed .footer-bottom {
      flex-direction: column;
      gap: 8px;
      padding: 10px 8px 16px;
      align-items: center;
    }

    .theme-toggle {
      background: var(--bg-elevated);
      border: 1px solid var(--border);
      cursor: pointer;
      color: var(--text-muted);
      padding: 6px 8px;
      border-radius: 6px;
      transition: all 0.15s;
      display: flex;
      align-items: center;
    }
    .theme-toggle:hover { color: var(--accent); border-color: var(--border-hover); }

    .user-info {
      display: flex;
      align-items: center;
      gap: 10px;
      overflow: hidden;
    }
    .user-avatar {
      width: 34px;
      height: 34px;
      border-radius: 6px;
      background: var(--accent-muted);
      border: 1px solid var(--border-hover);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 13px;
      color: var(--accent);
      text-transform: uppercase;
      flex-shrink: 0;
    }
    .user-details {
      display: flex;
      flex-direction: column;
      overflow: hidden;
      opacity: 1;
      transition: opacity 0.2s;
    }
    .collapsed .user-details { opacity: 0; width: 0; }

    .user-name {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-secondary);
      white-space: nowrap;
    }
    .user-role {
      font-size: 11px;
      color: var(--text-muted);
      text-transform: lowercase;
    }
    .logout-btn {
      background: none;
      border: none;
      cursor: pointer;
      color: var(--text-muted);
      padding: 6px;
      border-radius: 6px;
      transition: all 0.15s;
      display: flex;
      align-items: center;
      flex-shrink: 0;
    }
    .logout-btn:hover { background: var(--bg-hover); color: var(--accent); }
  `]
})
export class SidebarComponent {
  collapsed = signal(this.loadState());
  collapsedChange = output<boolean>();

  constructor(public auth: AuthService, public themeService: ThemeService) {}

  userInitial(): string {
    return this.auth.currentUser()?.username?.charAt(0) || '?';
  }

  toggleSidebar(): void {
    this.collapsed.update(v => !v);
    localStorage.setItem('jp-sidebar', this.collapsed() ? 'collapsed' : 'expanded');
    this.collapsedChange.emit(this.collapsed());
  }

  private loadState(): boolean {
    return localStorage.getItem('jp-sidebar') === 'collapsed';
  }
}
