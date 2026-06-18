import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  undoFn?: () => void;
  leaving?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 0;

  show(message: string, type: Toast['type'] = 'info', undoFn?: () => void): void {
    const id = ++this.nextId;
    this.toasts.update(t => [...t, { id, message, type, undoFn }]);
    setTimeout(() => this.dismiss(id), 5000);
  }

  dismiss(id: number): void {
    this.toasts.update(t =>
      t.map(toast => (toast.id === id ? { ...toast, leaving: true } : toast))
    );
    setTimeout(() => {
      this.toasts.update(t => t.filter(toast => toast.id !== id));
    }, 220);
  }
}