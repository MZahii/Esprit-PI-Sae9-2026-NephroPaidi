import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

@Component({
  selector: 'app-backoffice-doctor',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './doctor.component.html',
  styleUrls: ['./doctor.component.scss']
})
export class DoctorComponent {
  role = 'DOCTOR';
  roleBadgeClass = 'bg-soft-warning text-warning';
  displayName = 'Doctor';
  displayEmail = 'doctor@clinic.tn';

  quickActions: Array<{
    label: string;
    description: string;
    route?: string;
    icon: string;
    disabled?: boolean;
  }> = [
    {
      label: 'Appointments',
      description: 'Open your calendar and upcoming visits.',
      route: '/backoffice/doctor/today',
      icon: 'feather-calendar'
    },
    {
      label: 'Consultations',
      description: 'Review and update consultation sessions.',
      route: '/backoffice/consultations',
      icon: 'feather-activity'
    },
    {
      label: 'Patient Summary',
      description: 'Coming soon',
      icon: 'feather-user',
      disabled: true
    }
  ];

  constructor(private authStorage: AuthStorageService) {
    const user = this.authStorage.getUser();
    const role = this.authStorage.getRole();
    if (role) {
      this.role = role;
      this.roleBadgeClass = (role === 'ADMIN' || role === 'PLATFORM_ADMIN')
        ? 'bg-soft-primary text-primary'
        : 'bg-soft-warning text-warning';
    }

    if (user?.firstName && user?.lastName) {
      this.displayName = `${user.firstName} ${user.lastName}`;
    } else if (user?.username) {
      this.displayName = user.username;
    }
    if (user?.email) {
      this.displayEmail = user.email;
    }
  }
}
