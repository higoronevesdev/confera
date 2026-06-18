import { Component, inject, signal } from '@angular/core';
import { FileDropzoneComponent } from '../../shared/components/file-dropzone/file-dropzone.component';
import { StatusPillComponent, PillStatus } from '../../shared/components/status-pill/status-pill.component';
import { ToastService } from '../../core/services/toast.service';

interface ImportRecord {
  id: string;
  fileName: string;
  fileType: 'CNAB240' | 'CNAB400' | 'OFX';
  status: PillStatus;
  totalRecords: number;
  createdAt: Date;
}

@Component({
  selector: 'app-imports',
  templateUrl: './imports.page.html',
  styleUrl: './imports.page.scss',
  imports: [FileDropzoneComponent, StatusPillComponent]
})
export class ImportsPage {
  private readonly toast = inject(ToastService);

  readonly history = signal<ImportRecord[]>([
    { id: '1', fileName: 'extrato-bradesco-jun-2026.cnab240', fileType: 'CNAB240', status: 'MATCHED',   totalRecords: 1847, createdAt: new Date('2026-06-17T09:30:00') },
    { id: '2', fileName: 'retorno-cielo-17062026.cnab400',    fileType: 'CNAB400', status: 'PENDING',   totalRecords: 342,  createdAt: new Date('2026-06-17T14:22:00') },
    { id: '3', fileName: 'ofx-nubank-maio-2026.ofx',          fileType: 'OFX',     status: 'UNMATCHED', totalRecords: 0,    createdAt: new Date('2026-06-16T11:15:00') },
  ]);

  onFileSelected(file: File): void {
    const name = file.name.toLowerCase();
    const fileType: ImportRecord['fileType'] =
      name.endsWith('.ofx') ? 'OFX' :
      name.includes('400')  ? 'CNAB400' : 'CNAB240';

    const newRec: ImportRecord = {
      id: Date.now().toString(),
      fileName: file.name,
      fileType,
      status: 'PENDING',
      totalRecords: 0,
      createdAt: new Date(),
    };

    this.history.update(h => [newRec, ...h]);
    this.toast.show(`"${file.name}" recebido. Processando…`, 'info');

    setTimeout(() => {
      this.history.update(h =>
        h.map(r => r.id === newRec.id
          ? { ...r, status: 'MATCHED' as PillStatus, totalRecords: Math.floor(Math.random() * 500 + 100) }
          : r
        )
      );
      this.toast.show(`"${file.name}" processado com sucesso!`, 'success');
    }, 3000);
  }

  statusLabel(s: PillStatus): string {
    const m: Record<PillStatus, string> = { MATCHED: 'Concluído', FUZZY: 'Parcial', UNMATCHED: 'Erro', PENDING: 'Processando' };
    return m[s];
  }

  formatDate(d: Date): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit', month: '2-digit', year: '2-digit',
      hour: '2-digit', minute: '2-digit',
    }).format(d);
  }
}