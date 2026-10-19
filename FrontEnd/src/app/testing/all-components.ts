import { DoctorTodayAppointmentsPage } from '../features/clinical/appointments/doctor-today-appointments.page';
import { ReceptionistAppointmentsPage } from '../features/clinical/appointments/receptionist-appointments.page';
import { ClinicalAlertCenterComponent } from '../features/clinical/consultations/components/clinical-alert-center.component';
import { ClinicalAssessmentFormComponent } from '../features/clinical/consultations/components/clinical-assessment-form.component';
import { DispositionPanelComponent } from '../features/clinical/consultations/components/disposition-panel.component';
import { EncounterHeaderComponent } from '../features/clinical/consultations/components/encounter-header.component';
import { FollowUpRequestDialogComponent } from '../features/clinical/consultations/components/follow-up-request-dialog.component';
import { HospitalizationActionDialogComponent } from '../features/clinical/consultations/components/hospitalization-action-dialog.component';
import { LabOrdersPanelComponent } from '../features/clinical/consultations/components/lab-orders-panel.component';
import { MedicationReviewPanelComponent } from '../features/clinical/consultations/components/medication-review-panel.component';
import { PatientSnapshotCardComponent } from '../features/clinical/consultations/components/patient-snapshot-card.component';
import { RenalMetricsPanelComponent } from '../features/clinical/consultations/components/renal-metrics-panel.component';
import { ConsultationDetailsPage } from '../features/clinical/consultations/consultation-details.page';
import { ConsultationLabSourcePage } from '../features/clinical/consultations/consultation-lab-source.page';
import { ConsultationWorkspacePage } from '../features/clinical/consultations/consultation-workspace.page';
import { ConsultationsListPage } from '../features/clinical/consultations/consultations-list.page';
import { DoctorPatientsPage } from '../features/clinical/consultations/doctor-patients.page';
import { LabRequestsPage } from '../features/clinical/consultations/lab-requests.page';
import { OpsDossierDetailPage } from '../features/clinical/ops/ops-dossier-detail.page';
import { OpsDossiersOverviewPage } from '../features/clinical/ops/ops-dossiers-overview.page';
import { OpsMyDossiersPage } from '../features/clinical/ops/ops-my-dossiers.page';
import { HospitalizationCreatePage } from '../features/ops/hospitalizations/hospitalization-create.page';
import { HospitalizationReviewPage } from '../features/ops/hospitalizations/hospitalization-review.page';
import { NurseHospitalizationsPage } from '../features/ops/hospitalizations/nurse-hospitalizations.page';
import { CalendarPage } from '../frontoffice/pages/calendar/calendar.page';
import { ConsultationDetailsPage as ConsultationDetailsPage25 } from '../frontoffice/pages/consultation-details/consultation-details.page';
import { ConsultationsPage } from '../frontoffice/pages/consultations/consultations.page';
import { MessagesPage } from '../frontoffice/pages/messages/messages.page';
import { ProfilePage } from '../frontoffice/pages/profile/profile.page';
import { SchedulePage } from '../frontoffice/pages/schedule/schedule.page';
import { BackofficeLayoutComponent } from '../layouts/backoffice-layout/backoffice-layout';
import { FrontofficeLayoutComponent } from '../layouts/frontoffice-layout/frontoffice-layout.component';
import { PublicLayoutComponent } from '../layouts/public-layout/public-layout';
import { AccountSettingsComponent } from '../pages/backoffice/account-settings/account-settings';
import { AppointmentsRequestsComponent } from '../pages/backoffice/appointments-requests/appointments-requests';
import { Appointments } from '../pages/backoffice/appointments/appointments';
import { ClinicalAuditLogsComponent } from '../pages/backoffice/clinical-audit-logs/clinical-audit-logs';
import { CommunicationAnalyticsComponent } from '../pages/backoffice/communication-analytics/communication-analytics';
import { CommunicationDetailsComponent } from '../pages/backoffice/communication-details/communication-details';
import { CommunicationInboxComponent } from '../pages/backoffice/communication-inbox/communication-inbox';
import { CommunicationTemplatesComponent } from '../pages/backoffice/communication-templates/communication-templates';
import { ContractsList } from '../pages/backoffice/contracts-list/contracts-list';
import { CreateContract } from '../pages/backoffice/create-contract/create-contract';
import { CreateGuardianPatient } from '../pages/backoffice/create-guardian-patient/create-guardian-patient';
import { CreateHr } from '../pages/backoffice/create-hr/create-hr';
import { CreateStaff } from '../pages/backoffice/create-staff/create-staff';
import { Dashboard } from '../pages/backoffice/dashboard/dashboard';
import { DischargeFollowUpComponent } from '../pages/backoffice/discharge-follow-up/discharge-follow-up';
import { DoctorDashboardComponent } from '../pages/backoffice/doctor-dashboard/doctor-dashboard';
import { DoctorComponent } from '../pages/backoffice/doctor/doctor.component';
import { EquipmentInventoryComponent } from '../pages/backoffice/equipment-inventory/equipment-inventory';
import { EquipmentPlacementComponent } from '../pages/backoffice/equipment-placement/equipment-placement';
import { ExistingGuardianPatient } from '../pages/backoffice/existing-guardian-patient/existing-guardian-patient';
import { GuardiansLinkedProfiles } from '../pages/backoffice/guardians-linked-profiles/guardians-linked-profiles';
import { HospitalStructureComponent } from '../pages/backoffice/hospital-structure/hospital-structure';
import { HrList } from '../pages/backoffice/hr-list/hr-list';
import { InternalStaffMessagingComponent } from '../pages/backoffice/internal-staff-messaging/internal-staff-messaging';
import { LabInboxComponent } from '../pages/backoffice/lab-inbox/lab-inbox';
import { LogsCenterComponent } from '../pages/backoffice/logs-center/logs-center';
import { LogsComponent } from '../pages/backoffice/logs/logs';
import { OfficeAssignmentsComponent } from '../pages/backoffice/office-assignments/office-assignments';
import { PatientsList } from '../pages/backoffice/patients-list/patients-list';
import { AlertsComponent } from '../pages/backoffice/pharmacy/alerts/alerts.component';
import { DialysisStockComponent } from '../pages/backoffice/pharmacy/dialysis-stock/dialysis-stock.component';
import { DispensationsComponent } from '../pages/backoffice/pharmacy/dispensations/dispensations.component';
import { EquipmentComponent } from '../pages/backoffice/pharmacy/equipment/equipment.component';
import { MedicationsComponent } from '../pages/backoffice/pharmacy/medications/medications.component';
import { MovementsComponent } from '../pages/backoffice/pharmacy/movements/movements.component';
import { PharmacyDashboardComponent } from '../pages/backoffice/pharmacy/pharmacy-dashboard/pharmacy-dashboard.component';
import { PrescriptionsComponent } from '../pages/backoffice/pharmacy/prescriptions/prescriptions.component';
import { StockComponent } from '../pages/backoffice/pharmacy/stock/stock.component';
import { SuppliersComponent } from '../pages/backoffice/pharmacy/suppliers/suppliers.component';
import { ProcedureDialysisOutcomesComponent } from '../pages/backoffice/procedure-dialysis-outcomes/procedure-dialysis-outcomes';
import { ProcedureDialysisPrescriptionsComponent } from '../pages/backoffice/procedure-dialysis-prescriptions/procedure-dialysis-prescriptions';
import { ProcedureDialysisSessionsComponent } from '../pages/backoffice/procedure-dialysis-sessions/procedure-dialysis-sessions';
import { ProcedureDialysisComponent } from '../pages/backoffice/procedure-dialysis/procedure-dialysis';
import { ProcedureSurgicalAdvancedComponent } from '../pages/backoffice/procedure-surgical-advanced/procedure-surgical-advanced';
import { ProcedureSurgicalComponent } from '../pages/backoffice/procedure-surgical/procedure-surgical';
import { StaffList } from '../pages/backoffice/staff-list/staff-list';
import { StaffPlacementsComponent } from '../pages/backoffice/staff-placements/staff-placements';
import { StaffRolesDetails } from '../pages/backoffice/staff-roles-details/staff-roles-details';
import { StaffUserDetails } from '../pages/backoffice/staff-user-details/staff-user-details';
import { GuardianAccountSettingsComponent } from '../pages/frontoffice/account-settings/account-settings';
import { FrontofficeAppointmentsComponent } from '../pages/frontoffice/appointments/appointments';
import { CommunicationListComponent } from '../pages/frontoffice/communication-list/communication-list';
import { CommunicationNewComponent } from '../pages/frontoffice/communication-new/communication-new';
import { CommunicationThreadComponent } from '../pages/frontoffice/communication-thread/communication-thread';
import { FrontofficeHomeComponent } from '../pages/frontoffice/frontoffice-home/frontoffice-home';
import { FrontofficePatientDetailsComponent } from '../pages/frontoffice/frontoffice-patient-details/frontoffice-patient-details';
import { FrontofficePharmacyComponent } from '../pages/frontoffice/frontoffice-pharmacy/frontoffice-pharmacy.component';
import { GuardianTrackingComponent } from '../pages/frontoffice/guardian-tracking/guardian-tracking';
import { FrontofficeProfileComponent } from '../pages/frontoffice/profile/profile';
import { AboutPageComponent } from '../pages/public/about-page/about-page';
import { ContactPageComponent } from '../pages/public/contact-page/contact-page';
import { DoctorsPageComponent } from '../pages/public/doctors-page/doctors-page';
import { FaqPageComponent } from '../pages/public/faq-page/faq-page';
import { HomePageComponent } from '../pages/public/home-page/home-page';
import { Login } from '../pages/public/login/login';
import { PrivacyPageComponent } from '../pages/public/privacy-page/privacy-page';
import { ServicesPageComponent } from '../pages/public/services-page/services-page';
import { TermsPageComponent } from '../pages/public/terms-page/terms-page';
import { MyContractComponent } from '../pages/shared/my-contract/my-contract';

export const ALL_COMPONENTS: Array<[string, unknown]> = [
  ['DoctorTodayAppointmentsPage', DoctorTodayAppointmentsPage],
  ['ReceptionistAppointmentsPage', ReceptionistAppointmentsPage],
  ['ClinicalAlertCenterComponent', ClinicalAlertCenterComponent],
  ['ClinicalAssessmentFormComponent', ClinicalAssessmentFormComponent],
  ['DispositionPanelComponent', DispositionPanelComponent],
  ['EncounterHeaderComponent', EncounterHeaderComponent],
  ['FollowUpRequestDialogComponent', FollowUpRequestDialogComponent],
  ['HospitalizationActionDialogComponent', HospitalizationActionDialogComponent],
  ['LabOrdersPanelComponent', LabOrdersPanelComponent],
  ['MedicationReviewPanelComponent', MedicationReviewPanelComponent],
  ['PatientSnapshotCardComponent', PatientSnapshotCardComponent],
  ['RenalMetricsPanelComponent', RenalMetricsPanelComponent],
  ['ConsultationDetailsPage', ConsultationDetailsPage],
  ['ConsultationLabSourcePage', ConsultationLabSourcePage],
  ['ConsultationWorkspacePage', ConsultationWorkspacePage],
  ['ConsultationsListPage', ConsultationsListPage],
  ['DoctorPatientsPage', DoctorPatientsPage],
  ['LabRequestsPage', LabRequestsPage],
  ['OpsDossierDetailPage', OpsDossierDetailPage],
  ['OpsDossiersOverviewPage', OpsDossiersOverviewPage],
  ['OpsMyDossiersPage', OpsMyDossiersPage],
  ['HospitalizationCreatePage', HospitalizationCreatePage],
  ['HospitalizationReviewPage', HospitalizationReviewPage],
  ['NurseHospitalizationsPage', NurseHospitalizationsPage],
  ['CalendarPage', CalendarPage],
  ['ConsultationDetailsPage25', ConsultationDetailsPage25],
  ['ConsultationsPage', ConsultationsPage],
  ['MessagesPage', MessagesPage],
  ['ProfilePage', ProfilePage],
  ['SchedulePage', SchedulePage],
  ['BackofficeLayoutComponent', BackofficeLayoutComponent],
  ['FrontofficeLayoutComponent', FrontofficeLayoutComponent],
  ['PublicLayoutComponent', PublicLayoutComponent],
  ['AccountSettingsComponent', AccountSettingsComponent],
  ['AppointmentsRequestsComponent', AppointmentsRequestsComponent],
  ['Appointments', Appointments],
  ['ClinicalAuditLogsComponent', ClinicalAuditLogsComponent],
  ['CommunicationAnalyticsComponent', CommunicationAnalyticsComponent],
  ['CommunicationDetailsComponent', CommunicationDetailsComponent],
  ['CommunicationInboxComponent', CommunicationInboxComponent],
  ['CommunicationTemplatesComponent', CommunicationTemplatesComponent],
  ['ContractsList', ContractsList],
  ['CreateContract', CreateContract],
  ['CreateGuardianPatient', CreateGuardianPatient],
  ['CreateHr', CreateHr],
  ['CreateStaff', CreateStaff],
  ['Dashboard', Dashboard],
  ['DischargeFollowUpComponent', DischargeFollowUpComponent],
  ['DoctorDashboardComponent', DoctorDashboardComponent],
  ['DoctorComponent', DoctorComponent],
  ['EquipmentInventoryComponent', EquipmentInventoryComponent],
  ['EquipmentPlacementComponent', EquipmentPlacementComponent],
  ['ExistingGuardianPatient', ExistingGuardianPatient],
  ['GuardiansLinkedProfiles', GuardiansLinkedProfiles],
  ['HospitalStructureComponent', HospitalStructureComponent],
  ['HrList', HrList],
  ['InternalStaffMessagingComponent', InternalStaffMessagingComponent],
  ['LabInboxComponent', LabInboxComponent],
  ['LogsCenterComponent', LogsCenterComponent],
  ['LogsComponent', LogsComponent],
  ['OfficeAssignmentsComponent', OfficeAssignmentsComponent],
  ['PatientsList', PatientsList],
  ['AlertsComponent', AlertsComponent],
  ['DialysisStockComponent', DialysisStockComponent],
  ['DispensationsComponent', DispensationsComponent],
  ['EquipmentComponent', EquipmentComponent],
  ['MedicationsComponent', MedicationsComponent],
  ['MovementsComponent', MovementsComponent],
  ['PharmacyDashboardComponent', PharmacyDashboardComponent],
  ['PrescriptionsComponent', PrescriptionsComponent],
  ['StockComponent', StockComponent],
  ['SuppliersComponent', SuppliersComponent],
  ['ProcedureDialysisOutcomesComponent', ProcedureDialysisOutcomesComponent],
  ['ProcedureDialysisPrescriptionsComponent', ProcedureDialysisPrescriptionsComponent],
  ['ProcedureDialysisSessionsComponent', ProcedureDialysisSessionsComponent],
  ['ProcedureDialysisComponent', ProcedureDialysisComponent],
  ['ProcedureSurgicalAdvancedComponent', ProcedureSurgicalAdvancedComponent],
  ['ProcedureSurgicalComponent', ProcedureSurgicalComponent],
  ['StaffList', StaffList],
  ['StaffPlacementsComponent', StaffPlacementsComponent],
  ['StaffRolesDetails', StaffRolesDetails],
  ['StaffUserDetails', StaffUserDetails],
  ['GuardianAccountSettingsComponent', GuardianAccountSettingsComponent],
  ['FrontofficeAppointmentsComponent', FrontofficeAppointmentsComponent],
  ['CommunicationListComponent', CommunicationListComponent],
  ['CommunicationNewComponent', CommunicationNewComponent],
  ['CommunicationThreadComponent', CommunicationThreadComponent],
  ['FrontofficeHomeComponent', FrontofficeHomeComponent],
  ['FrontofficePatientDetailsComponent', FrontofficePatientDetailsComponent],
  ['FrontofficePharmacyComponent', FrontofficePharmacyComponent],
  ['GuardianTrackingComponent', GuardianTrackingComponent],
  ['FrontofficeProfileComponent', FrontofficeProfileComponent],
  ['AboutPageComponent', AboutPageComponent],
  ['ContactPageComponent', ContactPageComponent],
  ['DoctorsPageComponent', DoctorsPageComponent],
  ['FaqPageComponent', FaqPageComponent],
  ['HomePageComponent', HomePageComponent],
  ['Login', Login],
  ['PrivacyPageComponent', PrivacyPageComponent],
  ['ServicesPageComponent', ServicesPageComponent],
  ['TermsPageComponent', TermsPageComponent],
  ['MyContractComponent', MyContractComponent]
];
