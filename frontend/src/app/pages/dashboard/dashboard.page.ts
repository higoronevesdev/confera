import { Component, inject, signal, computed } from '@angular/core';
import { forkJoin } from 'rxjs';
import { MetricCardComponent } from '../../shared/components/metric-card/metric-card.component';
import { StatusPillComponent, PillStatus } from '../../shared/components/status-pill/status-pill.component';
import { LedgerService, AccountResponse } from '../../core/services/ledger.service';
import { ReconciliationService, DiscrepancyResponse, MatchResponse } from '../../core/services/reconciliation.service';

interface ActivityRow {
  id: string;
  description: string;
  amountCents: number;
  date: string;
  status: PillStatus;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
  imports: [MetricCardComponent, StatusPillComponent]
})
export class DashboardPage {
  private readonly ledger          = inject(LedgerService);
  private readonly reconciliation  = inject(ReconciliationService);

  readonly loading         = signal(true);
  readonly accounts        = signal<AccountResponse[]>([]);
  readonly discrepancies   = signal<DiscrepancyResponse[]>([]);
  readonly matches         = signal<MatchResponse[]>([]);

  readonly totalBalanceCents = computed(() =>
    this.accounts()
      .filter(a => a.type === 'ASSET')
      .reduce((sum, a) => sum + a.balanceCents, 0)
  );

  readonly openDiscrepancyCount = computed(() =>
    this.discrepancies().filter(d => d.status === 'OPEN').length
  );

  readonly matchedTodayCents = computed(() => {
    const today = new Date().toDateString();
    return this.matches()
      .filter(m => new Date(m.createdAt).toDateString() === today)
      .reduce((sum, m) => sum + Math.abs(m.differenceCents ?? 0), 0);
  });

  readonly recentActivity = computed<ActivityRow[]>(() => {
    const discActivity: ActivityRow[] = this.discrepancies().slice(0, 3).map(d => ({
      id: d.id.slice(0, 8),
      description: d.description || d.type,
      amountCents: d.amountCents,
      date: d.createdAt,
      status: d.status === 'RESOLVED' ? 'MATCHED' : 'UNMATCHED' as PillStatus,
    }));

    const matchActivity: ActivityRow[] = this.matches().slice(0, 2).map(m => ({
      id: m.id.slice(0, 8),
      description: `Conciliação ${m.matchType}`,
      amountCents: Math.abs(m.differenceCents ?? 0),
      date: m.createdAt,
      status: m.matchType === 'EXACT' ? 'MATCHED' : m.matchType === 'FUZZY' ? 'FUZZY' : 'MATCHED' as PillStatus,
    }));

    return [...matchActivity, ...discActivity].slice(0, 5);
  });

  readonly metrics = computed(() => [
    {
      label: 'Posição de caixa',
      value: this.totalBalanceCents() / 100,
      prefix: 'R$',
      trend: undefined,
      trendLabel: '',
      decimals: 2,
    },
    {
      label: 'Contas ativas',
      value: this.accounts().length,
      prefix: '',
      trend: undefined,
      trendLabel: '',
      decimals: 0,
    },
    {
      label: 'Correspondências',
      value: this.matches().length,
      prefix: '',
      trend: undefined,
      trendLabel: '',
      decimals: 0,
    },
    {
      label: 'Divergências',
      value: this.openDiscrepancyCount(),
      prefix: '',
      trend: undefined,
      trendLabel: this.openDiscrepancyCount() > 0 ? 'requerem atenção' : 'tudo conciliado',
      decimals: 0,
    },
  ]);

  readonly chartData   = [0, 0, 0, 0, 0, 0, 0];
  readonly chartLabels = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];

  constructor() {
    forkJoin({
      accounts:      this.ledger.getAccounts(),
      discrepancies: this.reconciliation.listDiscrepancies(),
      matches:       this.reconciliation.listMatches(),
    }).subscribe({
      next: ({ accounts, discrepancies, matches }) => {
        this.accounts.set(accounts);
        this.discrepancies.set(discrepancies.content);
        this.matches.set(matches.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  get chartLinePath(): string { return this.buildPath(false); }
  get chartFillPath(): string { return this.buildPath(true); }

  private buildPath(close: boolean): string {
    const W = 600, H = 120, P = 16;
    const data = this.chartData;
    const min = Math.min(...data) * 0.92 || 0;
    const max = Math.max(...data) * 1.05 || 1;
    const rng = max - min || 1;
    const step = (W - P * 2) / (data.length - 1);
    const pts = data.map((v, i) => ({
      x: P + i * step,
      y: H - P - ((v - min) / rng) * (H - P * 2),
    }));

    let d = `M ${pts[0].x} ${pts[0].y}`;
    for (let i = 1; i < pts.length; i++) {
      const cp = (pts[i - 1].x + pts[i].x) / 2;
      d += ` C ${cp} ${pts[i - 1].y}, ${cp} ${pts[i].y}, ${pts[i].x} ${pts[i].y}`;
    }
    if (close) d += ` L ${pts[pts.length - 1].x} ${H} L ${pts[0].x} ${H} Z`;
    return d;
  }

  formatCurrency(cents: number): string {
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Math.abs(cents) / 100);
  }

  formatDate(d: string): string {
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' }).format(new Date(d));
  }
}