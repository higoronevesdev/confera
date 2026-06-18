import { Component, inject, signal } from '@angular/core';
import { FileDropzoneComponent } from '../../shared/components/file-dropzone/file-dropzone.component';
import { StatusPillComponent, PillStatus } from '../../shared/components/status-pill/status-pill.component';
import { ToastService } from '../../core/services/toast.service';
import { IngestionService, FileImportResponse, ImportStatus } from '../../core/services/ingestion.service';

const STATUS_PILL: Record<ImportStatus, PillStatus> = {
  PENDING:    'PENDING',
  PROCESSING: 'PENDING',
  DONE:       'MATCHED',
  ERROR:      'UNMATCHED',
};

const STATUS_LABEL: Record<ImportStatus, string> = {
  PENDING:    'Aguardando',
  PROCESSING: 'Processando',
  DONE:       'Concluído',
  ERROR:      'Erro',
};

@Component({
  selector: 'app-imports',
  templateUrl: './imports.page.html',
  styleUrl: './imports.page.scss',
  imports: [FileDropzoneComponent, StatusPillComponent]
})
export class ImportsPage {
  private readonly toast     = inject(ToastService);
  private readonly ingestion = inject(IngestionService);

  readonly loading = signal(true);
  readonly history = signal<FileImportResponse[]>([]);

  constructor() {
    this.loadImports();
  }

  private loadImports(): void {
    this.loading.set(true);
    this.ingestion.listImports().subscribe({
      next: page => {
        this.history.set(page.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onFileSelected(file: File): void {
    this.ingestion.uploadFile(file).subscribe({
      next: rec => {
        this.history.update(h => [rec, ...h]);
        this.toast.show(`"${file.name}" enviado para processamento.`, 'info');
      },
      error: () => {},
    });
  }

  pillStatus(s: ImportStatus): PillStatus { return STATUS_PILL[s] ?? 'PENDING'; }
  statusLabel(s: ImportStatus): string    { return STATUS_LABEL[s] ?? s; }

  formatDate(d: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit', month: '2-digit', year: '2-digit',
      hour: '2-digit', minute: '2-digit',
    }).format(new Date(d));
  }
}