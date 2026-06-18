import { Component, computed, inject, signal } from '@angular/core';
import { StatusPillComponent } from '../../shared/components/status-pill/status-pill.component';
import { ToastService } from '../../core/services/toast.service';

type RecStatus = 'MATCHED' | 'FUZZY' | 'UNMATCHED';

interface RecItem {
  id: string;
  description: string;
  expectedAmount: number;
  actualAmount?: number;
  delta?: number;
  acquirer: string;
  date: Date;
  status: RecStatus;
  confidence?: number;
}

@Component({
  selector: 'app-reconciliation',
  templateUrl: './reconciliation.page.html',
  styleUrl: './reconciliation.page.scss',
  imports: [StatusPillComponent]
})
export class ReconciliationPage {
  private readonly toast = inject(ToastService);

  readonly statusFilters = [
    { value: 'ALL',       label: 'Todos'          },
    { value: 'UNMATCHED', label: 'Não conciliado' },
    { value: 'FUZZY',     label: 'Parcial'        },
    { value: 'MATCHED',   label: 'Conciliado'     },
  ];

  readonly activeFilter   = signal<string>('ALL');
  readonly search         = signal<string>('');
  readonly matchedExpanded = signal<boolean>(false);
  readonly selectedItem   = signal<RecItem | null>(null);

  readonly items = signal<RecItem[]>([
    { id: 'R-001', description: 'Cielo — Venda #4532',          expectedAmount: 1500.00, actualAmount: 1500.00, delta: 0,      acquirer: 'Cielo',     date: new Date('2026-06-17'), status: 'MATCHED',   confidence: 100 },
    { id: 'R-002', description: 'Stone — MDR divergente #7712',  expectedAmount: 2300.00, actualAmount: 2287.50, delta: -12.50, acquirer: 'Stone',     date: new Date('2026-06-16'), status: 'FUZZY',     confidence: 87  },
    { id: 'R-003', description: 'PagSeguro — Crédito faltante',  expectedAmount:  890.00, actualAmount: undefined,              acquirer: 'PagSeguro', date: new Date('2026-06-15'), status: 'UNMATCHED'                   },
    { id: 'R-004', description: 'Rede — Taxa indevida #9043',    expectedAmount:    0.00, actualAmount: 45.80,   delta: 45.80,  acquirer: 'Rede',      date: new Date('2026-06-15'), status: 'UNMATCHED'                   },
    { id: 'R-005', description: 'Getnet — Parcelamento lote 24', expectedAmount: 3600.00, actualAmount: 3612.00, delta: 12.00,  acquirer: 'Getnet',    date: new Date('2026-06-14'), status: 'FUZZY',     confidence: 72  },
    { id: 'R-006', description: 'Cielo — Débito #7891',          expectedAmount:  750.00, actualAmount: 750.00,  delta: 0,      acquirer: 'Cielo',     date: new Date('2026-06-14'), status: 'MATCHED',   confidence: 100 },
    { id: 'R-007', description: 'Stone — Antecipação',           expectedAmount: 5000.00, actualAmount: 5000.00, delta: 0,      acquirer: 'Stone',     date: new Date('2026-06-13'), status: 'MATCHED',   confidence: 100 },
  ]);

  readonly matchRate = computed(() => {
    const all = this.items();
    return all.length ? Math.round((all.filter(i => i.status === 'MATCHED').length / all.length) * 100) : 0;
  });

  readonly filtered = computed(() => {
    const f = this.activeFilter();
    const q = this.search().toLowerCase().trim();
    return this.items().filter(i =>
      (f === 'ALL' || i.status === f) &&
      (!q || i.description.toLowerCase().includes(q) || i.acquirer.toLowerCase().includes(q))
    );
  });

  readonly grouped = computed(() => ({
    unmatched: this.filtered().filter(i => i.status === 'UNMATCHED'),
    fuzzy:     this.filtered().filter(i => i.status === 'FUZZY'),
    matched:   this.filtered().filter(i => i.status === 'MATCHED'),
  }));

  selectItem(item: RecItem): void { this.selectedItem.set(item); }
  closeDrawer(): void { this.selectedItem.set(null); }

  acceptItem(item: RecItem, e: Event): void {
    e.stopPropagation();
    const prev = { ...item };
    this.items.update(list =>
      list.map(i => i.id === item.id ? { ...i, status: 'MATCHED' as RecStatus, confidence: 100 } : i)
    );
    this.toast.show(`"${item.description}" aceito como conciliado.`, 'success', () => {
      this.items.update(list => list.map(i => i.id === prev.id ? prev : i));
    });
  }

  rejectItem(item: RecItem, e: Event): void {
    e.stopPropagation();
    const prev = { ...item };
    this.items.update(list =>
      list.map(i => i.id === item.id ? { ...i, status: 'UNMATCHED' as RecStatus, confidence: undefined } : i)
    );
    this.toast.show(`"${item.description}" marcado como divergente.`, 'warning', () => {
      this.items.update(list => list.map(i => i.id === prev.id ? prev : i));
    });
  }

  formatCurrency(v: number): string {
    return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v);
  }

  formatDate(d: Date): string {
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(d);
  }
}