import { Component, computed, inject, signal } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';

type DiscType = 'FEE_DIVERGENCE' | 'MISSING_CREDIT' | 'UNEXPECTED_DEBIT' | 'CHARGEBACK';

interface Discrepancy {
  id: string;
  type: DiscType;
  description: string;
  amount: number;
  date: Date;
  ignored?: boolean;
}

const TYPE_META: Record<DiscType, { label: string; mod: string }> = {
  FEE_DIVERGENCE:   { label: 'Taxa Divergente',  mod: 'fee'      },
  MISSING_CREDIT:   { label: 'Crédito Faltante', mod: 'missing'  },
  UNEXPECTED_DEBIT: { label: 'Débito Indevido',  mod: 'debit'    },
  CHARGEBACK:       { label: 'Chargeback',        mod: 'chargeback'},
};

@Component({
  selector: 'app-discrepancies',
  templateUrl: './discrepancies.page.html',
  styleUrl: './discrepancies.page.scss'
})
export class DiscrepanciesPage {
  private readonly toast = inject(ToastService);

  readonly items = signal<Discrepancy[]>([
    { id: 'D-001', type: 'MISSING_CREDIT',   description: 'Crédito não localizado — PagSeguro venda #8821', amount:  890.00, date: new Date('2026-06-15') },
    { id: 'D-004', type: 'CHARGEBACK',        description: 'Chargeback cliente #3341 — Cielo',               amount:  450.00, date: new Date('2026-06-12') },
    { id: 'D-002', type: 'UNEXPECTED_DEBIT',  description: 'Taxa MDR não prevista — Rede #9043',             amount:   45.80, date: new Date('2026-06-15') },
    { id: 'D-003', type: 'FEE_DIVERGENCE',    description: 'Divergência de taxa — Stone venda #7712',        amount:   12.50, date: new Date('2026-06-16') },
    { id: 'D-005', type: 'FEE_DIVERGENCE',    description: 'MDR divergente — Getnet lote 2406',              amount:   12.00, date: new Date('2026-06-14') },
  ]);

  readonly pendingId    = signal<string | null>(null);
  readonly pendingType  = signal<'resolve' | 'ignore' | null>(null);

  readonly sorted = computed(() =>
    [...this.items()].sort((a, b) => b.amount - a.amount)
  );

  typeMeta(t: DiscType) { return TYPE_META[t]; }

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
      this.toast.show(`Divergência resolvida com sucesso.`, 'success', () => {
        this.items.update(list => [orig, ...list].sort((a, b) => b.amount - a.amount));
      });
    } else {
      this.items.update(list => list.map(i => i.id === id ? { ...i, ignored: true } : i));
      this.toast.show(`Divergência ignorada.`, 'info', () => {
        this.items.update(list => list.map(i => i.id === id ? { ...i, ignored: false } : i));
      });
    }

    this.cancelAction();
  }

  formatCurrency(v: number): string {
    return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v);
  }

  formatDate(d: Date): string {
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(d);
  }
}