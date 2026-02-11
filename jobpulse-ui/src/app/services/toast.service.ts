import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface Toast {
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
  duration?: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toastSubject = new Subject<Toast>();
  toast$ = this.toastSubject.asObservable();

  success(message: string): void {
    this.toastSubject.next({ message, type: 'success', duration: 3000 });
  }

  error(message: string): void {
    this.toastSubject.next({ message, type: 'error', duration: 5000 });
  }

  info(message: string): void {
    this.toastSubject.next({ message, type: 'info', duration: 3000 });
  }

  warning(message: string): void {
    this.toastSubject.next({ message, type: 'warning', duration: 4000 });
  }
}
