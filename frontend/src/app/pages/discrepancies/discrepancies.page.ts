import { Component, computed, inject, signal } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';
import {
  ReconciliationService,
  DiscrepancyResponse,
  DiscrepancyType,
  DiscrepancyStatus,
} from '../../core/services/reconciliation.service';

const TYPE_META: Record<DiscrepancyType, { label: string; mod: string }> = {
  FEE_DIVERGENCE:   { label: 'Taxa Divergente',  mod: 'fee'       },
  MISSING_CREDIT:   { label: 'Crédito Faltante', mod: 'missing'   },
  UNEXPECTED_DEBIT: { label: 'Débito Indevido',  mod: 'debit'     },
  DATE_DIVERGENCE:  { label: 'Data Divergente',  mod: 'date'      },
  CHARGEBACK:       { label: 'Chargeback',        mod: 'chargeback'},
};

@Component({
  selector: 'app-discrepancies',
  templateUrl: './discrepancies.page.html',
  styleUrl: './discrepancies.page.scss'
})
export class DiscrepanciesPage {
  private readonly toast = inject(ToastService);
  private readonly svc   = inject(ReconciliationService);

  readonly loading     = signal(true);
  readonly items       = signal<DiscrepancyResponse[]>([]);
  readonly pendingId   = signal<string | null>(null);
  readonly pendingType = signal<'resolve' | 'ignore' | null>(null);

  readonly sorted = computed(() =>
    [...this.items()].sort((a, b) => b.amountCents - a.amountCents)
  );

  readonly open = computed(() =>
    this.sorted().filter(i => i.status === 'OPEN' || i.status === 'INVESTIGATING')
  );

  constructor() {
    this.loadDiscrepancies();
  }

  private loadDiscrepancies(): void {
    this.loading.set(true);
    this.svc.listDiscrepancies('OPEN').subscribe({
      next: page => {
        this.items.set(page.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  typeMeta(t: DiscrepancyType) { return TYPE_META[t]; }

  startAction(id: string, type: 'resolve' | 'ignore', e: Event): void {
    e.stopPropagation();
    this.pendingId.set(id);
    this.pendingType.set(type);
  }

  cancelAction(): void {
    this.pendingId.set(null);
    this.pendingType.set(null);
  }

  confirmAction(): void {
    const id   = this.pendingId()!;
    const type = this.pendingType()!;
    const orig = this.items().find(i => i.id === id)!;

    if (type === 'resolve') {
      this.items.update(list => list.filter(i => i.id !== id));
      this.svc.resolveDiscrepancy(id, { status: 'RESOLVED' }).subscribe({
        next: () => this.toast.show('Divergência resolvida com sucesso.', 'success'),
        error: () => {
          this.items.update(list => [orig, ...list]);
          this.toast.show('Falha ao resolver. Tente novamente.', 'error');
        },
      });
    } else {
      this.items.update(list =>
        list.map(i => i.id === id ? { ...i, status: 'IGNORED' as DiscrepancyStatus } : i)
      );
      this.svc.resolveDiscrepancy(id, { status: 'IGNORED' }).subscribe({
        next: () => this.toast.show('Divergência ignorada.', 'info'),
        error: () => {
          this.items.update(list => list.map(i => i.id === id ? orig : i));
          this.toast.show('Falha ao ignorar. Tente novamente.', 'error');
        },
      });
    }

    this.cancelAction();
  }

  formatCurrency(cents: number): string {
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Math.abs(cents) / 100);
  }

  formatDate(d: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    }).format(new Date(d));
  }
}