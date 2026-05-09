import { Component } from '@angular/core';
import { Appointments } from '../../../pages/backoffice/appointments/appointments';

@Component({
  selector: 'app-receptionist-appointments',
  standalone: true,
  imports: [Appointments],
  template: '<app-appointments></app-appointments>'
})
export class ReceptionistAppointmentsPage {}
