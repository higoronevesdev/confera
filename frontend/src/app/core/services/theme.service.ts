import { Injectable, effect, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<'dark' | 'light'>(
    (localStorage.getItem('theme') as 'dark' | 'light') ?? 'dark'
  );

  constructor() {
    effect(() => {
      const t = this.theme();
      if (t === 'light') {
        document.documentElement.setAttribute('data-theme', 'light');
      } else {
        document.documentElement.removeAttribute('data-theme');
      }
      localStorage.setItem('theme', t);
    });
  }

  toggle(): void {
    this.theme.update(t => (t === 'dark' ? 'light' : 'dark'));
  }
}