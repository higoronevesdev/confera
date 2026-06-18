import { Component, ElementRef, ViewChild, computed, output, signal } from '@angular/core';

@Component({
  selector: 'app-file-dropzone',
  templateUrl: './file-dropzone.component.html',
  styleUrl: './file-dropzone.component.scss'
})
export class FileDropzoneComponent {
  readonly fileSelected = output<File>();

  readonly selectedFile = signal<File | null>(null);
  readonly progress     = signal(0);
  readonly isDragOver   = signal(false);
  readonly progressInt  = computed(() => Math.round(this.progress()));

  @ViewChild('fileInput') private fileInputRef!: ElementRef<HTMLInputElement>;

  private dragCount = 0;

  openPicker(): void {
    this.fileInputRef.nativeElement.click();
  }

  onDragEnter(e: DragEvent): void {
    e.preventDefault();
    this.dragCount++;
    this.isDragOver.set(true);
  }

  onDragLeave(): void {
    this.dragCount--;
    if (this.dragCount <= 0) {
      this.dragCount = 0;
      this.isDragOver.set(false);
    }
  }

  onDragOver(e: DragEvent): void {
    e.preventDefault();
  }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    this.dragCount = 0;
    this.isDragOver.set(false);
    const file = e.dataTransfer?.files[0];
    if (file) this.handleFile(file);
  }

  onFileInputChange(e: Event): void {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) this.handleFile(file);
  }

  cancel(): void {
    this.selectedFile.set(null);
    this.progress.set(0);
    this.fileInputRef.nativeElement.value = '';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private handleFile(file: File): void {
    this.selectedFile.set(file);
    this.progress.set(0);
    let p = 0;
    const iv = setInterval(() => {
      p += Math.random() * 18 + 5;
      if (p >= 100) {
        clearInterval(iv);
        this.progress.set(100);
        setTimeout(() => this.fileSelected.emit(file), 300);
      } else {
        this.progress.set(p);
      }
    }, 120);
  }
}