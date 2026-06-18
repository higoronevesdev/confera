import { Component, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LedgerService, AccountResponse, EntryResponse } from '../../core/services/ledger.service';

@Component({
  selector: 'app-ledger',
  templateUrl: './ledger.page.html',
  styleUrl: './ledger.page.scss',
  imports: [FormsModule]
})
export class LedgerPage {
  private readonly svc = inject(LedgerService);

  readonly loadingAccounts  = signal(true);
  readonly loadingStatement = signal(false);
  readonly accounts         = signal<AccountResponse[]>([]);
  readonly entries          = signal<EntryResponse[]>([]);
  readonly selectedCode     = signal<string>('');

  readonly selectedAccount = computed(() =>
    this.accounts().find(a => a.code === this.selectedCode()) ?? null
  );

  readonly dateFrom = signal(this.defaultFrom());
  readonly dateTo   = signal(this.today());

  constructor() {
    this.svc.getAccounts().subscribe({
      next: list => {
        this.accounts.set(list);
        if (list.length) {
          this.selectedCode.set(list[0].code);
          this.loadStatement();
        }
        this.loadingAccounts.set(false);
      },
      error: () => this.loadingAccounts.set(false),
    });
  }

  onAccountChange(code: string): void {
    this.selectedCode.set(code);
    this.entries.set([]);
    this.loadStatement();
  }

  loadStatement(): void {
    const code = this.selectedCode();
    if (!code) return;
    this.loadingStatement.set(true);
    this.svc.getStatement(code, this.dateFrom(), this.dateTo()).subscribe({
      next: page => {
        this.entries.set(page.content);
        this.loadingStatement.set(false);
      },
      error: () => this.loadingStatement.set(false),
    });
  }

  accountTypeLabel(type: string): string {
    const m: Record<string, string> = {
      ASSET:     'Ativo',
      LIABILITY: 'Passivo',
      EQUITY:    'Patrimônio',
      REVENUE:   'Receita',
      EXPENSE:   'Despesa',
    };
    return m[type] ?? type;
  }

  formatCurrency(cents: number): string {
    return new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Math.abs(cents) / 100);
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private defaultFrom(): string {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().slice(0, 10);
  }
}