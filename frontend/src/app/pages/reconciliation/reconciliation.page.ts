import { Component, computed, inject, signal } from '@angular/core';
import { StatusPillComponent } from '../../shared/components/status-pill/status-pill.component';
import { ToastService } from '../../core/services/toast.service';
import {
  ReconciliationService,
  DiscrepancyResponse,
  DiscrepancyType,
} from '../../core/services/reconciliation.service';

type RecStatus = 'MATCHED' | 'FUZZY' | 'UNMATCHED';

interface RecItem {
  id: string;
  description: string;
  amountCents: number;
  type: DiscrepancyType;
  date: string;
  status: RecStatus;
  apiStatus: string;
}

const TYPE_LABEL: Record<DiscrepancyType, string> = {
  FEE_DIVERGENCE:   'Taxa Divergente',
  MISSING_CREDIT:   'Crédito Faltante',
  UNEXPECTED_DEBIT: 'Débito Indevido',
  DATE_DIVERGENCE:  'Data Divergente',
  CHARGEBACK:       'Chargeback',
};

function toRecItem(d: DiscrepancyResponse): RecItem {
  const status: RecStatus =
    d.status === 'RESOLVED' ? 'MATCHED' :
    d.status === 'INVESTIGATING' ? 'FUZZY' : 'UNMATCHED';
  return {
    id:          d.id,
    description: d.description || TYPE_LABEL[d.type],
    amountCents: d.amountCents,
    type:        d.type,
    date:        d.createdAt,
    status,
    apiStatus:   d.status,
  };
}

@Component({
  selector: 'app-reconciliation',
  templateUrl: './reconciliation.page.html',
  styleUrl: './reconciliation.page.scss',
  imports: [StatusPillComponent]
})
export class ReconciliationPage {
  private readonly toast   = inject(ToastService);
  private readonly svc     = inject(ReconciliationService);

  readonly statusFilters = [
    { value: 'ALL',          label: 'Todos'          },
    { value: 'UNMATCHED',    label: 'Não conciliado' },
    { value: 'FUZZY',        label: 'Parcial'        },
    { value: 'MATCHED',      label: 'Conciliado'     },
  ];

  readonly activeFilter    = signal<string>('ALL');
  readonly search          = signal<string>('');
  readonly matchedExpanded = signal<boolean>(false);
  readonly selectedItem    = signal<RecItem | null>(null);
  readonly loading         = signal<boolean>(true);
  readonly running         = signal<boolean>(false);

  readonly items = signal<RecItem[]>([]);

  readonly matchRate = computed(() => {
    const all = this.items();
    if (!all.length) return 0;
    return Math.round((all.filter(i => i.status === 'MATCHED').length / all.length) * 100);
  });

  readonly filtered = computed(() => {
    const f = this.activeFilter();
    const q = this.search().toLowerCase().trim();
    return this.items().filter(i =>
      (f === 'ALL' || i.status === f) &&
      (!q || i.description.toLowerCase().includes(q) || TYPE_LABEL[i.type].toLowerCase().includes(q))
    );
  });

  readonly grouped = computed(() => ({
    unmatched: this.filtered().filter(i => i.status === 'UNMATCHED'),
    fuzzy:     this.filtered().filter(i => i.status === 'FUZZY'),
    matched:   this.filtered().filter(i => i.status === 'MATCHED'),
  }));

  constructor() {
    this.loadDiscrepancies();
  }

  private loadDiscrepancies(): void {
    this.loading.set(true);
    this.svc.listDiscrepancies().subscribe({
      next: page => {
        this.items.set(page.content.map(toRecItem));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  runReconciliation(): void {
    this.running.set(true);
    this.svc.runReconciliation().subscribe({
      next: summary => {
        this.running.set(false);
        this.toast.show(
          `Conciliação concluída: ${summary.exactMatches} exatos, ${summary.fuzzyMatches} parciais, ${summary.unmatched} sem par.`,
          'success'
        );
        this.loadDiscrepancies();
      },
      error: () => this.running.set(false),
    });
  }

  selectItem(item: RecItem): void { this.selectedItem.set(item); }
  closeDrawer(): void             { this.selectedItem.set(null); }

  acceptItem(item: RecItem, e: Event): void {
    e.stopPropagation();
    const prev = { ...item };
    this.items.update(list =>
      list.map(i => i.id === item.id ? { ...i, status: 'MATCHED' as RecStatus, apiStatus: 'RESOLVED' } : i)
    );
    this.svc.resolveDiscrepancy(item.id, { status: 'RESOLVED' }).subscribe({
      next: () => this.toast.show(`"${item.description}" aceito como conciliado.`, 'success'),
      error: () => {
        this.items.update(list => list.map(i => i.id === prev.id ? prev : i));
      },
    });
  }

  rejectItem(item: RecItem, e: Event): void {
    e.stopPropagation();
    const prev = { ...item };
    this.items.update(list =>
      list.map(i => i.id === item.id ? { ...i, status: 'UNMATCHED' as RecStatus, apiStatus: 'IGNORED' } : i)
    );
    this.svc.resolveDiscrepancy(item.id, { status: 'IGNORED' }).subscribe({
      next: () => this.toast.show(`"${item.description}" marcado como ignorado.`, 'warning'),
      error: () => {
        this.items.update(list => list.map(i => i.id === prev.id ? prev : i));
      },
    });
  }

  typeLabel(t: DiscrepancyType): string { return TYPE_LABEL[t]; }

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