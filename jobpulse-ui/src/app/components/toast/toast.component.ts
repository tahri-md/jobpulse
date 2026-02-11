import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ToastService, Toast } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toasts; track toast.id) {
        <div class="toast toast-{{ toast.type }}" (click)="removeToast(toast.id)">
          <div class="toast-icon">
            @switch (toast.type) {
              @case ('success') { <span>&#10003;</span> }
              @case ('error') { <span>&#10005;</span> }
              @case ('warning') { <span>!</span> }
              @case ('info') { <span>i</span> }
            }
          </div>
          <span class="toast-message">{{ toast.message }}</span>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .toast {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 20px;
      border-radius: 6px;
      font-size: 14px;
      cursor: pointer;
      animation: slideIn 0.3s ease;
      min-width: 280px;
      border: 1px solid var(--toast-border);
      box-shadow: var(--shadow-md);
    }
    .toast-success { background: var(--toast-bg); color: var(--text-secondary); border-left: 3px solid var(--success); }
    .toast-error { background: var(--toast-bg); color: var(--text-secondary); border-left: 3px solid var(--danger); }
    .toast-warning { background: var(--toast-bg); color: var(--text-secondary); border-left: 3px solid var(--accent); }
    .toast-info { background: var(--toast-bg); color: var(--text-secondary); border-left: 3px solid var(--text-muted); }
    .toast-icon {
      font-size: 14px;
      font-weight: 700;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
    }
    .toast-success .toast-icon { color: var(--success); }
    .toast-error .toast-icon { color: var(--danger); }
    .toast-warning .toast-icon { color: var(--accent); }
    .toast-info .toast-icon { color: var(--text-muted); }
    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
  `]
})
export class ToastComponent implements OnInit, OnDestroy {
  toasts: { id: number; message: string; type: string }[] = [];
  private sub!: Subscription;
  private counter = 0;

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.sub = this.toastService.toast$.subscribe((toast: Toast) => {
      const id = ++this.counter;
      this.toasts.push({ id, message: toast.message, type: toast.type });
      setTimeout(() => this.removeToast(id), toast.duration || 3000);
    });
  }

  removeToast(id: number): void {
    this.toasts = this.toasts.filter(t => t.id !== id);
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
