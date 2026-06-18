import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AccountResponse {
  id: string;
  name: string;
  code: string;
  type: string;
  normalBalance: 'DEBIT' | 'CREDIT';
  balanceCents: number;
}

export interface BalanceResponse {
  accountId: string;
  accountCode: string;
  accountName: string;
  balanceCents: number;
  totalDebitCents: number;
  totalCreditCents: number;
}

export interface EntryResponse {
  id: string;
  accountCode: string;
  accountName: string;
  amountCents: number;
  direction: 'DEBIT' | 'CREDIT';
}

export interface TransactionResponse {
  id: string;
  description: string;
  occurredAt: string;
  entries: EntryResponse[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PostTransactionRequest {
  idempotencyKey: string;
  description: string;
  entries: { accountCode: string; direction: 'DEBIT' | 'CREDIT'; amountCents: number }[];
}

@Injectable({ providedIn: 'root' })
export class LedgerService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/ledger`;

  getAccounts(): Observable<AccountResponse[]> {
    return this.http.get<AccountResponse[]>(`${this.base}/accounts`);
  }

  getBalance(code: string): Observable<BalanceResponse> {
    return this.http.get<BalanceResponse>(`${this.base}/accounts/${code}/balance`);
  }

  getStatement(code: string, from: string, to: string, page = 0): Observable<Page<EntryResponse>> {
    return this.http.get<Page<EntryResponse>>(
      `${this.base}/accounts/${code}/statement`,
      { params: { from, to, page: page.toString() } }
    );
  }

  postTransaction(request: PostTransactionRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.base}/transactions`, request);
  }

  reverseTransaction(id: string): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.base}/transactions/${id}/reverse`, {});
  }
}