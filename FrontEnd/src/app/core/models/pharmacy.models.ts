// ─── Medications ─────────────────────────────────────────────────────────────

export interface Medication {
  medicationId?: number;
  name: string;
  genericName?: string;
  form: string;
  strength?: string;
  unit?: string;
  therapeuticClass?: string;
  standardDosage?: string;
  renalDoseAdjustment?: boolean;
  storageConditions?: string;
  controlledSubstance?: boolean;
  minimumStock?: number | null;
  pediatricDosage?: string;
}

export type StorageCondition = 'ROOM_TEMPERATURE' | 'REFRIGERATED_2_8' | 'FROZEN' | 'LIGHT_PROTECTED';

export interface ReorderAlert {
  medicationId: number;
  name: string;
  form: string;
  minimumStock: number;
  currentTotalStock: number;
  deficit: number;
}

export interface SmartDispenseRequest {
  medicationId: number;
  quantity: number;
  patientId?: number;
  patientName?: string;
  prescriptionId?: number;
  dispensedBy?: string;
}

export interface SmartDispenseResponse {
  medicationId: number;
  medicationName: string;
  requested: number;
  totalDispensed: number;
  lines: { batchId: number; batchNumber: string; quantityDispensed: number; expirationDate: string }[];
}

export interface TransferStockRequest {
  sourceBatchId: number;
  targetBatchId: number;
  quantity: number;
  reason: string;
}

export interface TransferStockResponse {
  sourceBatchId: number;
  targetBatchId: number;
  quantityTransferred: number;
  sourceQuantityAvailable: number;
  targetQuantityAvailable: number;
  reason: string;
}

export interface Batch {
  batchId?: number;
  batchNumber: string;
  manufactureDate?: string;
  expirationDate: string;
  quantity: number;
  medicationId?: number;
  expired?: boolean;
}

export interface Stock {
  stockId?: number;
  batchId: number;
  quantityAvailable: number;
}

export interface DispenseRequest {
  batchId: number;
  quantity: number;
  patientId?: number;
  patientName?: string;
  prescriptionId?: number;
  dispensedBy?: string;
}

export interface Supplier {
  supplierId?: number;
  name: string;
  contactInfo: string;
  email?: string;
  isActive?: boolean;
}

export interface SupplierStats {
  supplierId: number;
  supplierName: string;
  totalOrders: number;
  deliveredOrders: number;
  pendingOrders: number;
  cancelledOrders: number;
  overdueOrders: number;
  deliveryRate: number;
}

// ─── Dispensation Log ─────────────────────────────────────────────────────────

export interface DispensationLog {
  id?: number;
  batchId: number;
  batchNumber?: string;
  medicationName?: string;
  quantity: number;
  patientId?: number;
  patientName?: string;
  prescriptionId?: number;
  dispensedBy?: string;
  dispensedAt: string;
}

export interface SupplyOrder {
  orderId?: number;
  supplierId?: number;
  medicationId?: number;
  itemType?: 'MEDICATION' | 'EQUIPMENT' | 'DIALYSIS';
  itemId?: number;
  itemName?: string;
  orderDate?: string;
  status?: 'PENDING' | 'DELIVERED' | 'CANCELLED';
  orderedQuantity: number;
  deliveredQuantity?: number;
  expectedDeliveryDate?: string;
  actualDeliveryDate?: string;
  notes?: string;
}

// ─── Equipment Stock ──────────────────────────────────────────────────────────

export interface EquipmentItem {
  itemId?: number;
  name: string;
  category?: string;
  unit?: string;
  minimumStock?: number;
  description?: string;
  currentStock?: number;
  lowStock?: boolean;
}

// ─── Dialysis Stock ───────────────────────────────────────────────────────────

export interface DialysisItem {
  itemId?: number;
  name: string;
  category?: string;
  unit?: string;
  minimumStock?: number;
  description?: string;
  currentStock?: number;
  lowStock?: boolean;
}

// ─── Stock Movements ─────────────────────────────────────────────────────────

export interface StockMovement {
  id?: number;
  stockType: 'EQUIPMENT' | 'DIALYSIS';
  itemId: number;
  itemName?: string;
  quantityTaken: number;
  requestedBy?: string;
  requestedByRole?: string;
  purpose?: string;
  takenAt?: string;
}

export interface RecordMovementRequest {
  stockType: 'EQUIPMENT' | 'DIALYSIS';
  itemId: number;
  quantityTaken: number;
  requestedBy?: string;
  requestedByRole?: string;
  purpose?: string;
}

// ─── Pharmacy Prescriptions ──────────────────────────────────────────────────

export interface PharmacyPrescription {
  id?: number;
  consultationId?: string;
  patientId?: number;
  patientName?: string;
  patientAge?: number;
  patientWeight?: number;
  doctorId?: string;
  doctorName?: string;
  urgency?: 'STAT' | 'URGENT' | 'ROUTINE';
  medicationsJson?: string;
  notes?: string;
  status?: 'PENDING' | 'PROCESSING' | 'DISPENSED' | 'CANCELLED';
  receivedAt?: string;
  verifiedBy?: string;
  verifiedAt?: string;
  allergyConfirmed?: boolean;
  processedAt?: string;
  processedBy?: string;
}
