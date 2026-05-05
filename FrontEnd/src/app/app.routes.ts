import { Routes } from '@angular/router';

import { PublicLayoutComponent } from './layouts/public-layout/public-layout';
import { BackofficeLayoutComponent } from './layouts/backoffice-layout/backoffice-layout';
import { FrontofficeLayoutComponent } from './layouts/frontoffice-layout/frontoffice-layout.component';

import { Dashboard } from './pages/backoffice/dashboard/dashboard';
import { CreateHr } from './pages/backoffice/create-hr/create-hr';
import { CreateStaff } from './pages/backoffice/create-staff/create-staff';
import { StaffList } from './pages/backoffice/staff-list/staff-list';
import { StaffUserDetails } from './pages/backoffice/staff-user-details/staff-user-details';
import { HrList } from './pages/backoffice/hr-list/hr-list';
import { StaffRolesDetails } from './pages/backoffice/staff-roles-details/staff-roles-details';
import { CreateGuardianPatient } from './pages/backoffice/create-guardian-patient/create-guardian-patient';
import { ExistingGuardianPatient } from './pages/backoffice/existing-guardian-patient/existing-guardian-patient';
import { PatientsList } from './pages/backoffice/patients-list/patients-list';
import { GuardiansLinkedProfiles } from './pages/backoffice/guardians-linked-profiles/guardians-linked-profiles';
import { ContractsList } from './pages/backoffice/contracts-list/contracts-list';
import { CreateContract } from './pages/backoffice/create-contract/create-contract';
import { FrontofficeHomeComponent } from './pages/frontoffice/frontoffice-home/frontoffice-home';
import { FrontofficePatientDetailsComponent } from './pages/frontoffice/frontoffice-patient-details/frontoffice-patient-details';
import { MyContractComponent } from './pages/shared/my-contract/my-contract';
import { LogsComponent } from './pages/backoffice/logs/logs';
import { ClinicalAuditLogsComponent } from './pages/backoffice/clinical-audit-logs/clinical-audit-logs';
import { CommunicationInboxComponent } from './pages/backoffice/communication-inbox/communication-inbox';
import { CommunicationDetailsComponent } from './pages/backoffice/communication-details/communication-details';
import { CommunicationTemplatesComponent } from './pages/backoffice/communication-templates/communication-templates';
import { CommunicationAnalyticsComponent } from './pages/backoffice/communication-analytics/communication-analytics';
import { AppointmentsRequestsComponent } from './pages/backoffice/appointments-requests/appointments-requests';
import { Appointments } from './pages/backoffice/appointments/appointments';
import { ProcedureDialysisComponent } from './pages/backoffice/procedure-dialysis/procedure-dialysis';
import { ProcedureDialysisOutcomesComponent } from './pages/backoffice/procedure-dialysis-outcomes/procedure-dialysis-outcomes';
import { ProcedureDialysisPrescriptionsComponent } from './pages/backoffice/procedure-dialysis-prescriptions/procedure-dialysis-prescriptions';
import { ProcedureDialysisSessionsComponent } from './pages/backoffice/procedure-dialysis-sessions/procedure-dialysis-sessions';
import { ProcedureSurgicalComponent } from './pages/backoffice/procedure-surgical/procedure-surgical';
import { ProcedureSurgicalAdvancedComponent } from './pages/backoffice/procedure-surgical-advanced/procedure-surgical-advanced';
import { HospitalStructureComponent } from './pages/backoffice/hospital-structure/hospital-structure';
import { EquipmentInventoryComponent } from './pages/backoffice/equipment-inventory/equipment-inventory';
import { EquipmentPlacementComponent } from './pages/backoffice/equipment-placement/equipment-placement';
import { OfficeAssignmentsComponent } from './pages/backoffice/office-assignments/office-assignments';
import { StaffPlacementsComponent } from './pages/backoffice/staff-placements/staff-placements';
import { AccountSettingsComponent } from './pages/backoffice/account-settings/account-settings';
import { GuardianTrackingComponent } from './pages/frontoffice/guardian-tracking/guardian-tracking';
import { FrontofficePharmacyComponent } from './pages/frontoffice/frontoffice-pharmacy/frontoffice-pharmacy.component';
import { CommunicationListComponent } from './pages/frontoffice/communication-list/communication-list';
import { CommunicationNewComponent } from './pages/frontoffice/communication-new/communication-new';
import { CommunicationThreadComponent } from './pages/frontoffice/communication-thread/communication-thread';
import { FrontofficeAppointmentsComponent } from './pages/frontoffice/appointments/appointments';
import { FrontofficeProfileComponent } from './pages/frontoffice/profile/profile';
import { DoctorComponent } from './pages/backoffice/doctor/doctor.component';
import { ConsultationsListPage } from './features/clinical/consultations/consultations-list.page';
import { ConsultationDetailsPage as ClinicalConsultationDetailsPage } from './features/clinical/consultations/consultation-details.page';
import { ConsultationWorkspacePage } from './features/clinical/consultations/consultation-workspace.page';
import { ReceptionistAppointmentsPage } from './features/clinical/appointments/receptionist-appointments.page';
import { DoctorTodayAppointmentsPage } from './features/clinical/appointments/doctor-today-appointments.page';
import { HospitalizationCreatePage } from './features/ops/hospitalizations/hospitalization-create.page';
import { HospitalizationReviewPage } from './features/ops/hospitalizations/hospitalization-review.page';
import { NurseHospitalizationsPage } from './features/ops/hospitalizations/nurse-hospitalizations.page';
import { CalendarPage } from './frontoffice/pages/calendar/calendar.page';
import { ConsultationsPage as GuardianConsultationsPage } from './frontoffice/pages/consultations/consultations.page';
import { ConsultationDetailsPage as GuardianConsultationDetailsPage } from './frontoffice/pages/consultation-details/consultation-details.page';
import { ProfilePage } from './frontoffice/pages/profile/profile.page';
import { MessagesPage } from './frontoffice/pages/messages/messages.page';
import { SchedulePage } from './frontoffice/pages/schedule/schedule.page';

import { HomePageComponent } from './pages/public/home-page/home-page';
import { AboutPageComponent } from './pages/public/about-page/about-page';
import { ServicesPageComponent } from './pages/public/services-page/services-page';
import { DoctorsPageComponent } from './pages/public/doctors-page/doctors-page';
import { FaqPageComponent } from './pages/public/faq-page/faq-page';
import { TermsPageComponent } from './pages/public/terms-page/terms-page';
import { PrivacyPageComponent } from './pages/public/privacy-page/privacy-page';
import { ContactPageComponent } from './pages/public/contact-page/contact-page';
import { Login } from './pages/public/login/login';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { passwordChangeGuard } from './core/guards/password-change.guard';

export const routes: Routes = [
  // ================= PUBLIC WEBSITE =================
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', component: HomePageComponent },
      { path: 'about', component: AboutPageComponent },
      { path: 'services', component: ServicesPageComponent },
      { path: 'doctors', component: DoctorsPageComponent },
      { path: 'faq', component: FaqPageComponent },
      { path: 'terms', component: TermsPageComponent },
      { path: 'privacy', component: PrivacyPageComponent },
      { path: 'contact', component: ContactPageComponent },
      { path: 'login', component: Login }
    ]
  },

  // ================= BACKOFFICE =================
  {
    path: 'backoffice',
    component: BackofficeLayoutComponent,
    canActivate: [authGuard, roleGuard, passwordChangeGuard],
    data: {
      roles: ['ADMIN', 'HR', 'DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT']
    },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: Dashboard },
      {
        path: 'create-hr',
        component: CreateHr,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'hr-list',
        component: HrList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'create-staff',
        component: CreateStaff,
        canActivate: [roleGuard],
        data: { roles: ['HR'] }
      },
      {
        path: 'staff',
        component: StaffList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR'] }
      },
      {
        path: 'staff/:id',
        component: StaffUserDetails,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR'] }
      },
      {
        path: 'staff-details',
        component: StaffRolesDetails,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR'] }
      },
      {
        path: 'create-guardian-patient',
        component: CreateGuardianPatient,
        canActivate: [roleGuard],
        data: { roles: ['RECEPTIONIST'] }
      },
      {
        path: 'existing-guardian-patient',
        component: ExistingGuardianPatient,
        canActivate: [roleGuard],
        data: { roles: ['RECEPTIONIST'] }
      },
      {
        path: 'patients',
        component: PatientsList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'RECEPTIONIST'] }
      },
      {
        path: 'guardians-linked',
        component: GuardiansLinkedProfiles,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'RECEPTIONIST'] }
      },
      {
        path: 'contracts',
        component: ContractsList,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR'] }
      },
      {
        path: 'contracts/create',
        component: CreateContract,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR'] }
      },
      {
        path: 'communication/inbox',
        component: CommunicationInboxComponent,
        canActivate: [roleGuard],
        data: { roles: ['RECEPTIONIST', 'NURSE', 'DOCTOR'] }
      },
      {
        path: 'communication/templates',
        component: CommunicationTemplatesComponent,
        canActivate: [roleGuard],
        data: { roles: ['RECEPTIONIST', 'NURSE', 'DOCTOR'] }
      },
      {
        path: 'communication/analytics',
        component: CommunicationAnalyticsComponent,
        canActivate: [roleGuard],
        data: { roles: ['RECEPTIONIST', 'NURSE', 'DOCTOR'] }
      },
      {
        path: 'communication/:id',
        component: CommunicationDetailsComponent,
        canActivate: [roleGuard],
        data: { roles: ['RECEPTIONIST', 'NURSE', 'DOCTOR'] }
      },
      {
        path: 'appointments/requests',
        component: AppointmentsRequestsComponent,
        canActivate: [roleGuard],
        data: { roles: ['RECEPTIONIST'] }
      },
      {
        path: 'appointments',
        component: Appointments,
        canActivate: [roleGuard],
        data: { roles: ['RECEPTIONIST'] }
      },
      {
        path: 'appointments-clinical',
        pathMatch: 'full',
        redirectTo: 'appointments'
      },
      // ================== DOCTOR CLINICAL WORKSPACE ==================
      // Canonical entry for doctors: /backoffice/doctor → redirects to /backoffice/doctor/today
      // From there, doctors can drill into consultations, lab-requests, and specific consultation details/workspaces.
      // All navigation preserves filters and page context via returnUrl query params (Step 6).
      // ===============================================================
      {
        path: 'doctor',
        pathMatch: 'full',
        redirectTo: 'doctor/today'
      },
      {
        path: 'doctor/today',
        component: DoctorTodayAppointmentsPage,
        canActivate: [roleGuard],
        data: { roles: ['DOCTOR'] }
      },
      {
        path: 'doctor/:id',
        component: DoctorComponent,
        canActivate: [roleGuard],
        data: { roles: ['DOCTOR'] }
      },
      {
        path: 'consultations',
        component: ConsultationsListPage,
        canActivate: [roleGuard],
        data: { roles: ['DOCTOR'] }
      },
      {
        path: 'consultations/:id',
        component: ClinicalConsultationDetailsPage,
        canActivate: [roleGuard],
        data: { roles: ['DOCTOR'] }
      },
      {
        path: 'consultations/:id/workspace',
        component: ConsultationWorkspacePage,
        canActivate: [roleGuard],
        data: { roles: ['DOCTOR'] }
      },
      {
        path: 'consultations/:id/hospitalization/new',
        component: HospitalizationCreatePage,
        canActivate: [roleGuard],
        data: { roles: ['DOCTOR'] }
      },
      {
        path: 'hospitalizations/:id',
        component: HospitalizationReviewPage,
        canActivate: [roleGuard],
        data: { roles: ['DOCTOR', 'NURSE'] }
      },
      {
        path: 'nurse/hospitalizations',
        component: NurseHospitalizationsPage,
        canActivate: [roleGuard],
        data: { roles: ['NURSE'] }
      },
      {
        path: 'my-contract',
        component: MyContractComponent,
        canActivate: [roleGuard],
        data: { roles: ['HR', 'DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'] }
      },
      {
        path: 'logs',
        component: LogsComponent,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'clinical-audit-logs',
        component: ClinicalAuditLogsComponent,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'procedures/dialysis',
        component: ProcedureDialysisComponent,
        canActivate: [roleGuard],
        data: { roles: ['SURGEON'] }
      },
      {
        path: 'procedures/dialysis-sessions',
        component: ProcedureDialysisSessionsComponent,
        canActivate: [roleGuard],
        data: { roles: ['SURGEON'] }
      },
      {
        path: 'procedures/dialysis-outcomes',
        component: ProcedureDialysisOutcomesComponent,
        canActivate: [roleGuard],
        data: { roles: ['SURGEON'] }
      },
      {
        path: 'procedures/dialysis-prescriptions',
        component: ProcedureDialysisPrescriptionsComponent,
        canActivate: [roleGuard],
        data: { roles: ['SURGEON'] }
      },
      {
        path: 'procedures/surgical',
        component: ProcedureSurgicalComponent,
        canActivate: [roleGuard],
        data: { roles: ['SURGEON'] }
      },
      {
        path: 'procedures/surgical-advanced',
        component: ProcedureSurgicalAdvancedComponent,
        canActivate: [roleGuard],
        data: { roles: ['SURGEON'] }
      },
      {
        path: 'pharmacy',
        loadChildren: () =>
          import('./pages/backoffice/pharmacy/pharmacy.routes')
            .then((m) => m.PHARMACY_ROUTES),
        canActivate: [roleGuard],
        data: { roles: ['PHARMACIST', 'ADMIN', 'NURSE'] }
      },
      {
        path: 'hospital-structure',
        component: HospitalStructureComponent,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'equipment-inventory',
        component: EquipmentInventoryComponent,
        canActivate: [roleGuard],
        data: { roles: ['HR'] }
      },
      {
        path: 'equipment-placement',
        component: EquipmentPlacementComponent,
        canActivate: [roleGuard],
        data: { roles: ['HR'] }
      },
      {
        path: 'office-assignments',
        component: OfficeAssignmentsComponent,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] }
      },
      {
        path: 'staff-placements',
        component: StaffPlacementsComponent,
        canActivate: [roleGuard],
        data: { roles: ['HR'] }
      },
      {
        path: 'account-settings',
        component: AccountSettingsComponent,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR', 'DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'] }
      }
    ]
  },

  // ================= FRONTOFFICE =================
  {
    path: 'frontoffice',
    component: FrontofficeLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['GUARDIAN'] },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'home' },
      { path: 'home', component: FrontofficeHomeComponent },
      {
        path: 'schedule',
        component: SchedulePage,
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'appointments' },
          { path: 'appointments', component: FrontofficeAppointmentsComponent },
          { path: 'calendar', component: CalendarPage }
        ]
      },
      { path: 'calendar', pathMatch: 'full', redirectTo: 'schedule/calendar' },
      { path: 'consultations', component: GuardianConsultationsPage },
      { path: 'consultations/:id', component: GuardianConsultationDetailsPage },
      { path: 'messages', component: MessagesPage },
      { path: 'communication', component: CommunicationListComponent },
      { path: 'communication/new', component: CommunicationNewComponent },
      { path: 'communication/:id', component: CommunicationThreadComponent },
      { path: 'appointments', pathMatch: 'full', redirectTo: 'schedule/appointments' },
      { path: 'profile', component: ProfilePage },
      { path: 'profile-legacy', component: FrontofficeProfileComponent },
      { path: 'patients/:id', component: FrontofficePatientDetailsComponent },
      { path: 'my-contract', component: MyContractComponent },
      { path: 'tracking', component: GuardianTrackingComponent },
      { path: 'pharmacy', component: FrontofficePharmacyComponent }
    ]
  },

  // ================= FALLBACK =================
  { path: '**', redirectTo: '' }
];
