import { Injectable, signal, effect } from '@angular/core';

export type Theme = 'dark' | 'light';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  theme = signal<Theme>(this.getSavedTheme());

  constructor() {
    effect(() => {
      const t = this.theme();
      document.documentElement.setAttribute('data-theme', t);
      localStorage.setItem('jp-theme', t);
    });
    // Apply immediately on service creation
    document.documentElement.setAttribute('data-theme', this.theme());
  }

  toggle(): void {
    this.theme.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  private getSavedTheme(): Theme {
    const saved = localStorage.getItem('jp-theme');
    if (saved === 'light' || saved === 'dark') return saved;
    return 'dark';
  }
}
