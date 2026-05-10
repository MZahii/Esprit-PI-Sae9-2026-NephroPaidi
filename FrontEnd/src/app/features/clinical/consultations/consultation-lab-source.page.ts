import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';

@Component({
  selector: 'app-consultation-lab-source-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './consultation-lab-source.page.html',
  styleUrl: './consultation-lab-source.page.scss'
})
export class ConsultationLabSourcePage implements OnInit, OnDestroy {
  consultationId = '';
  fileName = 'Lab source file';
  loading = true;
  error = '';
  objectUrl: string | null = null;
  safeObjectUrl: SafeResourceUrl | null = null;
  contentType = '';
  isImage = false;
  isPdf = false;

  constructor(
    private route: ActivatedRoute,
    private api: ClinicalApiService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading = false;
      this.error = 'Consultation id missing.';
      return;
    }
    this.consultationId = id;
    this.fileName = this.route.snapshot.queryParamMap.get('fileName') || this.fileName;
    this.loadSource();
  }

  ngOnDestroy(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
    }
  }

  private loadSource(): void {
    this.loading = true;
    this.error = '';
    this.api.downloadLatestConsultationLabResult(this.consultationId).subscribe({
      next: (blob) => {
        this.loading = false;
        this.contentType = blob.type || '';
        this.objectUrl = URL.createObjectURL(blob);
        this.safeObjectUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
        this.isPdf = this.contentType.includes('pdf');
        this.isImage = this.contentType.startsWith('image/');
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        if (error.status === 404) {
          this.error = 'No uploaded lab source is linked to this consultation yet.';
          return;
        }
        if (error.status === 410) {
          this.error = 'This lab source was recorded, but its file is not available in the shared environment. It needs to be re-uploaded from a shared stack.';
          return;
        }
        this.error = 'Unable to load the analyzed lab source file.';
      }
    });
  }
}
