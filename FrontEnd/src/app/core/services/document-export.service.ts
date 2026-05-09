import { Injectable } from '@angular/core';

export interface DocumentSummaryItem {
  label: string;
  value: string | number;
}

export interface DocumentTableColumn {
  key: string;
  label: string;
}

export interface DocumentExportConfig {
  title: string;
  subtitle?: string;
  generatedBy?: string;
  generatedAt?: Date;
  summary?: DocumentSummaryItem[];
  columns: DocumentTableColumn[];
  rows: Record<string, string | number | null | undefined>[];
  emptyText?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentExportService {
  printDocument(config: DocumentExportConfig): void {
    this.openDocument(config, true);
  }

  exportPdf(config: DocumentExportConfig): void {
    this.openDocument(config, true);
  }

  private openDocument(config: DocumentExportConfig, autoPrint: boolean): void {
    const html = this.buildDocumentHtml(config, autoPrint);
    const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const popup = window.open(url, '_blank');
    if (!popup) return;
    const cleanup = () => setTimeout(() => URL.revokeObjectURL(url), 10000);
    popup.addEventListener('load', cleanup, { once: true });
  }

  private buildDocumentHtml(config: DocumentExportConfig, autoPrint: boolean): string {
    const generatedAt = config.generatedAt ?? new Date();
    const logoUrl = `${window.location.origin}/assets/backoffice/images/logo/Logo_fin.png`;
    const summaryHtml = this.buildSummaryHtml(config.summary ?? []);
    const tableHtml = this.buildTableHtml(config.columns, config.rows, config.emptyText ?? 'No records found.');

    return `
      <!doctype html>
      <html>
        <head>
          <meta charset="utf-8" />
          <title>${this.escapeHtml(config.title)}</title>
          <style>
            :root {
              --brand: #0f4aa2;
              --brand-dark: #0b2f6b;
              --brand-soft: #eef4ff;
              --ink: #0f172a;
              --muted: #64748b;
              --line: #d0ddee;
              --surface: #ffffff;
            }

            * { box-sizing: border-box; }

            body {
              margin: 0;
              color: var(--ink);
              font-family: "Segoe UI", "Inter", Tahoma, Arial, sans-serif;
              font-size: 12px;
              line-height: 1.45;
              background: #f3f7fc;
            }

            .doc-shell {
              width: 100%;
              max-width: 1120px;
              margin: 0 auto;
              padding: 20px;
            }

            .doc-card {
              background: var(--surface);
              border: 1px solid var(--line);
              border-radius: 14px;
              overflow: hidden;
              box-shadow: 0 8px 30px rgba(15, 23, 42, .08);
            }

            .doc-header {
              padding: 16px 18px 14px;
              border-bottom: 2px solid var(--brand);
              background: linear-gradient(135deg, #f8fbff 0%, #eef4ff 100%);
              display: flex;
              justify-content: space-between;
              align-items: flex-start;
              gap: 16px;
            }

            .brand {
              display: flex;
              align-items: center;
              gap: 12px;
            }

            .brand img {
              width: 46px;
              height: 46px;
              object-fit: contain;
            }

            .clinic-name {
              margin: 0;
              font-size: 18px;
              font-weight: 800;
              color: var(--brand-dark);
            }

            .clinic-meta {
              margin: 2px 0 0;
              color: var(--muted);
              font-size: 11px;
            }

            .doc-title {
              margin: 8px 0 0;
              font-size: 22px;
              line-height: 1.2;
              color: #0b1739;
            }

            .doc-subtitle {
              margin: 4px 0 0;
              color: var(--muted);
              font-size: 12px;
            }

            .export-meta {
              text-align: right;
              color: var(--muted);
              font-size: 11px;
              min-width: 220px;
              display: grid;
              gap: 4px;
            }

            .meta-row {
              display: flex;
              justify-content: space-between;
              gap: 8px;
            }

            .doc-body {
              padding: 14px 18px;
            }

            .summary-grid {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
              gap: 10px;
              margin: 0 0 14px;
            }

            .summary-card {
              border: 1px solid var(--line);
              background: var(--brand-soft);
              border-radius: 10px;
              padding: 9px;
            }

            .summary-card .label {
              color: var(--muted);
              font-size: 10px;
              text-transform: uppercase;
              letter-spacing: .05em;
            }

            .summary-card .value {
              margin-top: 4px;
              font-size: 15px;
              font-weight: 800;
              color: #0f172a;
            }

            table {
              width: 100%;
              border-collapse: collapse;
              border: 1px solid var(--line);
              border-radius: 10px;
              overflow: hidden;
              table-layout: fixed;
            }

            thead th {
              text-align: left;
              padding: 10px 8px;
              background: #eaf1fd;
              color: #0b1739;
              font-size: 11px;
              border-bottom: 1px solid var(--line);
              word-break: break-word;
            }

            tbody td {
              padding: 8px;
              border-bottom: 1px solid #e9eff7;
              vertical-align: top;
              color: #1e293b;
              word-break: break-word;
            }

            tbody tr:nth-child(even) td {
              background: #f9fbff;
            }

            tbody tr:last-child td {
              border-bottom: none;
            }

            .empty {
              text-align: center;
              color: var(--muted);
              padding: 16px;
            }

            .doc-footer {
              border-top: 1px solid var(--line);
              padding: 10px 18px;
              display: flex;
              justify-content: space-between;
              align-items: center;
              color: var(--muted);
              font-size: 10px;
              background: #f8fbff;
            }

            .footer-left {
              display: grid;
              gap: 2px;
            }

            .footer-right {
              text-align: right;
            }

            .page-number:before {
              content: counter(page);
            }

            @page {
              size: A4;
              margin: 12mm;
            }

            @media print {
              body {
                background: #fff;
              }

              .doc-shell {
                max-width: none;
                padding: 0;
              }

              .doc-card {
                box-shadow: none;
                border-radius: 0;
              }

              .doc-footer {
                position: fixed;
                left: 0;
                right: 0;
                bottom: 0;
                border-top: 1px solid var(--line);
              }

              thead {
                display: table-header-group;
              }

              tr {
                page-break-inside: avoid;
              }
            }
          </style>
        </head>
        <body>
          <div class="doc-shell">
            <article class="doc-card">
              <header class="doc-header">
                <div>
                  <div class="brand">
                    <img src="${logoUrl}" alt="Clinic Logo" />
                    <div>
                      <h1 class="clinic-name">NephrosPaidi Clinic</h1>
                      <p class="clinic-meta">Hospital Management Platform • Administrative & Clinical Reports</p>
                    </div>
                  </div>
                  <h2 class="doc-title">${this.escapeHtml(config.title)}</h2>
                  ${config.subtitle ? `<p class="doc-subtitle">${this.escapeHtml(config.subtitle)}</p>` : ''}
                </div>
                <div class="export-meta">
                  <div class="meta-row"><strong>Generated At</strong><span>${this.escapeHtml(this.formatDateTime(generatedAt))}</span></div>
                  <div class="meta-row"><strong>Generated By</strong><span>${this.escapeHtml(config.generatedBy || 'System')}</span></div>
                  <div class="meta-row"><strong>Document Type</strong><span>Operational Report</span></div>
                </div>
              </header>

              <section class="doc-body">
                ${summaryHtml}
                ${tableHtml}
              </section>

              <footer class="doc-footer">
                <div class="footer-left">
                  <span>NephrosPaidi Clinic • Confidential</span>
                  <span>Prepared for administrative/clinical supervision workflows.</span>
                </div>
                <div class="footer-right">
                  <div>Page <span class="page-number"></span></div>
                  <div>${this.escapeHtml(this.formatDateTime(generatedAt))}</div>
                </div>
              </footer>
            </article>
          </div>
          <script>
            ${autoPrint ? 'setTimeout(function(){ window.focus(); window.print(); }, 350);' : ''}
          </script>
        </body>
      </html>
    `;
  }

  private buildSummaryHtml(summary: DocumentSummaryItem[]): string {
    if (!summary.length) return '';
    const cards = summary.map((item) => `
      <div class="summary-card">
        <div class="label">${this.escapeHtml(item.label)}</div>
        <div class="value">${this.escapeHtml(String(item.value))}</div>
      </div>
    `).join('');
    return `<div class="summary-grid">${cards}</div>`;
  }

  private buildTableHtml(
    columns: DocumentTableColumn[],
    rows: Record<string, string | number | null | undefined>[],
    emptyText: string
  ): string {
    const head = columns.map((column) => `<th>${this.escapeHtml(column.label)}</th>`).join('');
    if (!rows.length) {
      return `
        <table>
          <thead><tr>${head}</tr></thead>
          <tbody><tr><td class="empty" colspan="${columns.length}">${this.escapeHtml(emptyText)}</td></tr></tbody>
        </table>
      `;
    }

    const body = rows.map((row) => `
      <tr>
        ${columns.map((column) => `<td>${this.escapeHtml(String(row[column.key] ?? '-'))}</td>`).join('')}
      </tr>
    `).join('');

    return `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
  }

  private formatDateTime(date: Date): string {
    return new Intl.DateTimeFormat('en-GB', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).format(date);
  }

  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}
