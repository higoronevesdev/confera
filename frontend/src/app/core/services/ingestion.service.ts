import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from './ledger.service';

export type ImportStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'ERROR';
export type FileType = 'CNAB_240' | 'CNAB_400' | 'OFX' | 'CSV_ADQUIRENTE';

export interface FileImportResponse {
  id: string;
  fileName: string;
  fileType: FileType;
  status: ImportStatus;
  totalRecords: number | null;
  processedRecords: number | null;
  errorMessage: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class IngestionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/ingestion/imports`;

  uploadFile(file: File): Observable<FileImportResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<FileImportResponse>(this.base, form);
  }

  listImports(page = 0): Observable<Page<FileImportResponse>> {
    return this.http.get<Page<FileImportResponse>>(this.base, {
      params: { page: page.toString() }
    });
  }

  getImport(id: string): Observable<FileImportResponse> {
    return this.http.get<FileImportResponse>(`${this.base}/${id}`);
  }
}