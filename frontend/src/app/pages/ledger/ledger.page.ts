import { Component } from '@angular/core';

interface LedgerEntry {
  id: string;
  timestamp: Date;
  description: string;
  debitAccount: string;
  creditAccount: string;
  amount: number;
  balance: number;
}

@Component({
  selector: 'app-ledger',
  templateUrl: './ledger.page.html',
  styleUrl: './ledger.page.scss'
})
export class LedgerPage {
  readonly entries: LedgerEntry[] = [
    { id: 'LGR-00001', timestamp: new Date('2026-06-17T09:30:00'), description: 'Crédito Visa — Cielo batch #4532',    debitAccount: '1001 · Caixa',           creditAccount: '4001 · Receitas Cartão', amount: 1500.00,  balance: 48320.00 },
    { id: 'LGR-00002', timestamp: new Date('2026-06-17T08:15:00'), description: 'MDR Cielo — Taxa processamento',       debitAccount: '5001 · Taxas MDR',        creditAccount: '1001 · Caixa',           amount:   45.00,  balance: 46820.00 },
    { id: 'LGR-00003', timestamp: new Date('2026-06-16T16:45:00'), description: 'Crédito Stone — Venda #7712',          debitAccount: '1001 · Caixa',           creditAccount: '4001 · Receitas Cartão', amount: 3600.00,  balance: 46865.00 },
    { id: 'LGR-00004', timestamp: new Date('2026-06-16T14:22:00'), description: 'PIX recebido — cliente #2089',         debitAccount: '1001 · Caixa',           creditAccount: '4002 · Receitas PIX',    amount: 12000.00, balance: 43265.00 },
    { id: 'LGR-00005', timestamp: new Date('2026-06-15T11:30:00'), description: 'Estorno cliente #9012 — Cielo',        debitAccount: '4001 · Receitas Cartão', creditAccount: '1001 · Caixa',           amount:  450.00,  balance: 31265.00 },
    { id: 'LGR-00006', timestamp: new Date('2026-06-15T09:00:00'), description: 'Abertura — saldo anterior transferido', debitAccount: '1001 · Caixa',           creditAccount: '3001 · Capital',         amount: 31715.00, balance: 31715.00 },
  ];

  formatCurrency(v: number): string {
    return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v);
  }

  formatDateTime(d: Date): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit', month: '2-digit', year: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    }).format(d);
  }
}