import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from './ledger.service';

export type MatchType = 'EXACT' | 'FUZZY' | 'MANUAL';
export type DiscrepancyStatus = 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'IGNORED';
export type DiscrepancyType =
  | 'FEE_DIVERGENCE'
  | 'MISSING_CREDIT'
  | 'UNEXPECTED_DEBIT'
  | 'DATE_DIVERGENCE'
  | 'CHARGEBACK';

export interface ReconciliationSummaryResponse {
  tenantId: string;
  totalBankStatements: number;
  totalReceivables: number;
  exactMatches: number;
  fuzzyMatches: number;
  unmatched: number;
  discrepanciesCreated: number;
  executionTimeMs: number;
}

export interface MatchResponse {
  id: string;
  tenantId: string;
  bankStatementId: string;
  receivableId: string;
  matchType: MatchType;
  matchedByUserId: string | null;
  differenceCents: number;
  notes: string | null;
  createdAt: string;
}

export interface DiscrepancyResponse {
  id: string;
  tenantId: string;
  type: DiscrepancyType;
  bankStatementId: string | null;
  receivableId: string | null;
  amountCents: number;
  description: string;
  status: DiscrepancyStatus;
  resolvedAt: string | null;
  resolvedByUserId: string | null;
  createdAt: string;
}

export interface ResolveDiscrepancyRequest {
  status: 'RESOLVED' | 'IGNORED';
  notes?: string;
}

export interface ManualMatchRequest {
  bankStatementId: string;
  receivableId: string;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class ReconciliationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/reconciliation`;

  runReconciliation(): Observable<ReconciliationSummaryResponse> {
    return this.http.post<ReconciliationSummaryResponse>(`${this.base}/run`, {});
  }

  listMatches(page = 0): Observable<Page<MatchResponse>> {
    return this.http.get<Page<MatchResponse>>(`${this.base}/matches`, {
      params: { page: page.toString() }
    });
  }

  listDiscrepancies(status?: string, page = 0): Observable<Page<DiscrepancyResponse>> {
    const params: Record<string, string> = { page: page.toString() };
    if (status && status !== 'ALL') params['status'] = status;
    return this.http.get<Page<DiscrepancyResponse>>(`${this.base}/discrepancies`, { params });
  }

  resolveDiscrepancy(id: string, request: ResolveDiscrepancyRequest): Observable<DiscrepancyResponse> {
    return this.http.put<DiscrepancyResponse>(
      `${this.base}/discrepancies/${id}/resolve`,
      request
    );
  }

  createManualMatch(request: ManualMatchRequest): Observable<MatchResponse> {
    return this.http.post<MatchResponse>(`${this.base}/matches/manual`, request);
  }
}