import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

import { ClinicalAuditLogsComponent } from '../clinical-audit-logs/clinical-audit-logs';
import { LogsComponent } from '../logs/logs';

type LogsTab = 'audit' | 'clinical';

@Component({
  selector: 'app-logs-center',
  standalone: true,
  imports: [CommonModule, LogsComponent, ClinicalAuditLogsComponent],
  templateUrl: './logs-center.html',
  styleUrl: './logs-center.scss'
})
export class LogsCenterComponent {
  activeTab: LogsTab = 'audit';

  setTab(tab: LogsTab): void {
    this.activeTab = tab;
  }
}
