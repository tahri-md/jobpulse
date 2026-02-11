import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent],
  template: `
    <div class="layout">
      <app-sidebar (collapsedChange)="sidebarCollapsed.set($event)" />
      <main class="main-content" [class.collapsed]="sidebarCollapsed()">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .layout {
      display: flex;
      min-height: 100vh;
      background: var(--bg-root);
    }
    .main-content {
      flex: 1;
      margin-left: 260px;
      padding: 32px;
      overflow-y: auto;
      transition: margin-left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .main-content.collapsed {
      margin-left: 64px;
    }
  `]
})
export class LayoutComponent {
  sidebarCollapsed = signal(localStorage.getItem('jp-sidebar') === 'collapsed');
}
