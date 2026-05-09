import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';
import {
  CommunicationApiService,
  MessageType,
  QuickReplyTemplate
} from '../../../core/services/communication-api.service';

@Component({
  selector: 'app-communication-templates',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './communication-templates.html',
  styleUrl: './communication-templates.scss'
})
export class CommunicationTemplatesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  loading = false;
  saving = false;
  deletingId = '';
  errorMessage = '';

  templates: QuickReplyTemplate[] = [];
  editingId: string | null = null;

  readonly types: MessageType[] = ['ADMINISTRATIVE', 'APPOINTMENT', 'QUESTION', 'COMPLAINT', 'MEDICAL', 'LAB_RESULT', 'OTHER'];

  templateForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    messageType: ['ADMINISTRATIVE' as MessageType, [Validators.required]],
    templateText: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private communicationApi: CommunicationApiService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get title(): string {
    return this.editingId ? 'Edit template' : 'New template';
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.communicationApi.listTemplates()
      .pipe(timeout(10000))
      .pipe(finalize(() => {
        this.loading = false;
      }))
      .subscribe({
        next: (templates) => {
          this.templates = templates;
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Failed to load templates.';
        }
      });
  }

  startCreate(): void {
    this.editingId = null;
    this.templateForm.reset({
      name: '',
      messageType: 'ADMINISTRATIVE',
      templateText: ''
    });
  }

  startEdit(template: QuickReplyTemplate): void {
    this.editingId = template.id;
    this.templateForm.reset({
      name: template.name,
      messageType: template.messageType,
      templateText: template.templateText
    });
  }

  cancelEdit(): void {
    this.startCreate();
  }

  save(): void {
    if (this.templateForm.invalid || this.saving) {
      this.templateForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    const payload = {
      name: this.templateForm.value.name!.trim(),
      messageType: this.templateForm.value.messageType!,
      templateText: this.templateForm.value.templateText!.trim()
    };

    const request$ = this.editingId
      ? this.communicationApi.updateTemplate(this.editingId, payload)
      : this.communicationApi.createTemplate(payload);

    request$.pipe(finalize(() => {
      this.saving = false;
    })).subscribe({
      next: () => {
        this.startCreate();
        this.load();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to save template.';
      }
    });
  }

  deleteTemplate(template: QuickReplyTemplate): void {
    if (this.deletingId) {
      return;
    }
    this.deletingId = template.id;
    this.errorMessage = '';

    this.communicationApi.deleteTemplate(template.id)
      .pipe(finalize(() => {
        this.deletingId = '';
      }))
      .subscribe({
        next: () => {
          if (this.editingId === template.id) {
            this.startCreate();
          }
          this.load();
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Failed to delete template.';
        }
      });
  }
}
