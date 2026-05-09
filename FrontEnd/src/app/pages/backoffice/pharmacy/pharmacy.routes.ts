import { Routes } from '@angular/router';
import { PharmacyDashboardComponent } from './pharmacy-dashboard/pharmacy-dashboard.component';
import { MedicationsComponent } from './medications/medications.component';
import { StockComponent } from './stock/stock.component';
import { SuppliersComponent } from './suppliers/suppliers.component';
import { DispensationsComponent } from './dispensations/dispensations.component';
import { EquipmentComponent } from './equipment/equipment.component';
import { DialysisStockComponent } from './dialysis-stock/dialysis-stock.component';
import { MovementsComponent } from './movements/movements.component';
import { PrescriptionsComponent } from './prescriptions/prescriptions.component';
import { AlertsComponent } from './alerts/alerts.component';

export const PHARMACY_ROUTES: Routes = [
  { path: '',           redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard',  component: PharmacyDashboardComponent },
  { path: 'medications', component: MedicationsComponent },
  { path: 'stock',       component: StockComponent },
  { path: 'equipment',   component: EquipmentComponent },
  { path: 'dialysis-stock', component: DialysisStockComponent },
  { path: 'prescriptions',  component: PrescriptionsComponent },
  { path: 'dispensations',  component: DispensationsComponent },
  { path: 'movements',      component: MovementsComponent },
  { path: 'suppliers',      component: SuppliersComponent },
  { path: 'alerts',         component: AlertsComponent },
];
