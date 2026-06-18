import { Component } from '@angular/core';
import { MetricCardComponent } from '../../shared/components/metric-card/metric-card.component';
import { StatusPillComponent, PillStatus } from '../../shared/components/status-pill/status-pill.component';

interface Transaction {
  id: string;
  description: string;
  amount: number;
  date: Date;
  status: PillStatus;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
  imports: [MetricCardComponent, StatusPillComponent]
})
export class DashboardPage {
  readonly metrics = [
    { label: 'Posição de caixa', value: 48320, prefix: 'R$', trend: 12,        trendLabel: '+12% hoje',        decimals: 2 },
    { label: 'Conciliado hoje',  value: 31500, prefix: 'R$', trend: undefined,  trendLabel: '',                 decimals: 2 },
    { label: 'A receber',        value: 16820, prefix: 'R$', trend: undefined,  trendLabel: '',                 decimals: 2 },
    { label: 'Divergências',     value: 3,     prefix: '',   trend: -1,         trendLabel: '1 do dia ant.',    decimals: 0 },
  ];

  readonly chartData = [42000, 38000, 45000, 41000, 50000, 46000, 48320];
  readonly chartLabels = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];

  readonly lastTransactions: Transaction[] = [
    { id: 'TXN-001', description: 'Pagamento Visa 1234 — Cielo',       amount: -1250.00, date: new Date('2026-06-17'), status: 'MATCHED'   },
    { id: 'TXN-002', description: 'Crédito Mastercard 5678 — Stone',   amount:  3400.00, date: new Date('2026-06-16'), status: 'MATCHED'   },
    { id: 'TXN-003', description: 'Taxa MDR PagSeguro',                 amount:   -89.50, date: new Date('2026-06-16'), status: 'FUZZY'    },
    { id: 'TXN-004', description: 'Débito PIX recebido',                amount: 12000.00, date: new Date('2026-06-15'), status: 'UNMATCHED' },
    { id: 'TXN-005', description: 'Estorno cliente #9012',              amount:  -450.00, date: new Date('2026-06-15'), status: 'PENDING'   },
  ];

  get chartLinePath(): string { return this.buildPath(false); }
  get chartFillPath(): string { return this.buildPath(true); }

  private buildPath(close: boolean): string {
    const W = 600, H = 120, P = 16;
    const min = Math.min(...this.chartData) * 0.92;
    const max = Math.max(...this.chartData) * 1.05;
    const rng = max - min;
    const step = (W - P * 2) / (this.chartData.length - 1);
    const pts = this.chartData.map((v, i) => ({
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

  formatCurrency(v: number): string {
    return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Math.abs(v));
  }

  formatDate(d: Date): string {
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' }).format(d);
  }
}