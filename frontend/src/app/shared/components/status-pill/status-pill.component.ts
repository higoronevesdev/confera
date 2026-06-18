import { Component, computed, input } from '@angular/core';

export type PillStatus = 'MATCHED' | 'FUZZY' | 'UNMATCHED' | 'PENDING';

const CFG: Record<PillStatus, { label: string; mod: string }> = {
  MATCHED:   { label: 'Conciliado', mod: 'matched'   },
  FUZZY:     { label: 'Parcial',    mod: 'fuzzy'     },
  UNMATCHED: { label: 'Divergente', mod: 'unmatched' },
  PENDING:   { label: 'Pendente',   mod: 'pending'   },
};

@Component({
  selector: 'app-status-pill',
  template: `
    <span [class]="cls()">
      <span class="dot"></span>{{ cfg().label }}
    </span>
  `,
  styles: [`
    :host { display: inline-flex; }
    span {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 3px 10px; border-radius: var(--radius-pill);
      font-size: var(--fs-label); font-weight: var(--fw-label);
      letter-spacing: var(--ls-label); white-space: nowrap;
    }
    .dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }

    .pill--matched   { background: var(--success-tint); color: var(--success); border: 1px solid var(--success-border); }
    .pill--matched   .dot { background: var(--success); }
    .pill--fuzzy     { background: var(--warning-tint); color: var(--warning); border: 1px solid var(--warning-border); }
    .pill--fuzzy     .dot { background: var(--warning); }
    .pill--unmatched { background: var(--danger-tint);  color: var(--danger);  border: 1px solid var(--danger-border); }
    .pill--unmatched .dot { background: var(--danger); }
    .pill--pending   { background: transparent; color: var(--text-tertiary); border: 1px solid var(--border); }
    .pill--pending   .dot { background: var(--text-tertiary); }
  `]
})
export class StatusPillComponent {
  readonly status = input.required<PillStatus>();
  readonly cfg    = computed(() => CFG[this.status()]);
  readonly cls    = computed(() => `pill--${CFG[this.status()].mod}`);
}