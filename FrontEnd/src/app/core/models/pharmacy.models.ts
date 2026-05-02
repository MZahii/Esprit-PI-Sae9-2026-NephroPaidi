export interface Medication {
  medicationId?: number;
  name: string;
  form: string;
  pediatricDosage: string;
  minimumStock?: number | null;
}

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
  //patientId: string;
  //prescriptionId: string;
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

export interface DispensationLog {
  id?: number;
  batchId: number;
  quantity: number;
  dispensedAt: string;
}

export interface SupplyOrder {
  orderId?: number;
  supplierId?: number;
  medicationId: number;
  orderDate?: string;
  status?: 'PENDING' | 'DELIVERED' | 'CANCELLED';
  orderedQuantity: number;
  expectedDeliveryDate?: string;
  notes?: string;
}
