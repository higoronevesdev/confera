import { Component, inject } from '@angular/core';
import { Toast, ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  template: `
    <div class="toast-container">
      @for (toast of svc.toasts(); track toast.id) {
        <div class="toast toast--{{ toast.type }}" [class.leaving]="toast.leaving">
          <span class="toast__icon">
            @switch (toast.type) {
              @case ('success') { ✓ }
              @case ('error')   { ✕ }
              @case ('warning') { ⚠ }
              @default          { ℹ }
            }
          </span>
          <span class="toast__msg">{{ toast.message }}</span>
          @if (toast.undoFn) {
            <button class="toast__undo" (click)="undo(toast)">Desfazer</button>
          }
          <button class="toast__close" (click)="svc.dismiss(toast.id)" aria-label="Fechar">✕</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      bottom: 24px; right: 24px;
      display: flex; flex-direction: column; gap: 8px;
      z-index: var(--z-toast);
      pointer-events: none;
    }

    .toast {
      display: flex; align-items: center; gap: 10px;
      padding: 12px 16px;
      background: var(--surface-raised);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-lg);
      min-width: 280px; max-width: 420px;
      pointer-events: all;
      animation: tin 200ms ease-out;
      border-left-width: 3px;

      &.leaving { animation: tout 220ms ease-in forwards; }
    }

    @keyframes tin  { from { opacity:0; transform:translateX(20px); } to { opacity:1; transform:translateX(0); } }
    @keyframes tout { from { opacity:1; transform:translateX(0); } to { opacity:0; transform:translateX(20px); } }

    .toast--success { border-left-color: var(--success); }
    .toast--error   { border-left-color: var(--danger); }
    .toast--warning { border-left-color: var(--warning); }
    .toast--info    { border-left-color: var(--info); }

    .toast__icon { font-size: 14px; flex-shrink: 0; }
    .toast--success .toast__icon { color: var(--success); }
    .toast--error   .toast__icon { color: var(--danger); }
    .toast--warning .toast__icon { color: var(--warning); }
    .toast--info    .toast__icon { color: var(--info); }

    .toast__msg  { flex:1; font-size: var(--fs-body-sm); color: var(--text-primary); }

    .toast__undo {
      background: none; border: none;
      color: var(--brand-cyan-ink); font-size: var(--fs-body-sm); font-weight: 500;
      cursor: pointer; padding: 0; white-space: nowrap;
      &:hover { text-decoration: underline; }
    }

    .toast__close {
      background: none; border: none;
      color: var(--text-tertiary); cursor: pointer;
      font-size: 12px; line-height: 1; padding: 0;
      &:hover { color: var(--text-primary); }
    }
  `]
})
export class ToastComponent {
  readonly svc = inject(ToastService);

  undo(toast: Toast): void {
    toast.undoFn?.();
    this.svc.dismiss(toast.id);
  }
}