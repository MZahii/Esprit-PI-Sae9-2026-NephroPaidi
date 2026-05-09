import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { SuppliersComponent } from './suppliers.component';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { Supplier, SupplierStats, SupplyOrder } from '../../../../core/models/pharmacy.models';

describe('SuppliersComponent', () => {
  let component: SuppliersComponent;
  let fixture: ComponentFixture<SuppliersComponent>;

  const mockSuppliers: Supplier[] = [
    { supplierId: 1, name: 'PharmaCo',    contactInfo: 'info@pharma.com', isActive: true  },
    { supplierId: 2, name: 'MedSupply',   contactInfo: 'med@supply.com',  isActive: true  },
    { supplierId: 3, name: 'OldSupplier', contactInfo: 'old@co.com',      isActive: false },
  ];

  const mockStats: SupplierStats = {
    supplierId: 1, supplierName: 'PharmaCo',
    totalOrders: 5, deliveredOrders: 4, pendingOrders: 1,
    cancelledOrders: 0, overdueOrders: 0, deliveryRate: 80
  };

  const svcMock = {
    getSuppliers:          vi.fn(),
    getMedications:        vi.fn(),
    createSupplier:        vi.fn(),
    updateSupplier:        vi.fn(),
    deleteSupplier:        vi.fn(),
    getOrdersForSupplier:  vi.fn(),
    getSupplierStats:      vi.fn(),
    toggleSupplierStatus:  vi.fn(),
    placeOrder:            vi.fn(),
    markDelivered:         vi.fn(),
    cancelOrder:           vi.fn(),
  };

  beforeEach(async () => {
    svcMock.getSuppliers.mockReturnValue(of(mockSuppliers));
    svcMock.getMedications.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [SuppliersComponent],
      providers: [
        provideRouter([]),
        { provide: PharmacyService, useValue: svcMock }
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(SuppliersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.clearAllMocks());

  // ── Init ──────────────────────────────────────────────────────────────────

  it('should create and load suppliers on init', () => {
    expect(component).toBeTruthy();
    expect(component.suppliers().length).toBe(3);
    expect(svcMock.getSuppliers).toHaveBeenCalledTimes(1);
  });

  // ── Search (signal-based computed) ────────────────────────────────────────

  describe('search', () => {
    it('filters suppliers by name (case-insensitive)', () => {
      component.searchQuery.set('pharma');
      expect(component.filteredSuppliers().length).toBe(1);
      expect(component.filteredSuppliers()[0].name).toBe('PharmaCo');
    });

    it('filters by contactInfo', () => {
      component.searchQuery.set('med@supply');
      expect(component.filteredSuppliers().length).toBe(1);
      expect(component.filteredSuppliers()[0].name).toBe('MedSupply');
    });

    it('returns all when query is empty', () => {
      component.searchQuery.set('');
      expect(component.filteredSuppliers().length).toBe(3);
    });

    it('returns empty when no match', () => {
      component.searchQuery.set('zzzznotfound');
      expect(component.filteredSuppliers().length).toBe(0);
    });
  });

  // ── Status filter ─────────────────────────────────────────────────────────

  describe('statusFilter', () => {
    it('ACTIVE filter shows only active suppliers', () => {
      component.statusFilter.set('ACTIVE');
      const result = component.filteredSuppliers();
      expect(result.length).toBe(2);
      expect(result.every(s => s.isActive !== false)).toBe(true);
    });

    it('INACTIVE filter shows only inactive suppliers', () => {
      component.statusFilter.set('INACTIVE');
      const result = component.filteredSuppliers();
      expect(result.length).toBe(1);
      expect(result[0].name).toBe('OldSupplier');
    });

    it('ALL filter shows every supplier', () => {
      component.statusFilter.set('ALL');
      expect(component.filteredSuppliers().length).toBe(3);
    });
  });

  // ── Combined search + filter ──────────────────────────────────────────────

  it('combines search query and status filter', () => {
    component.statusFilter.set('ACTIVE');
    component.searchQuery.set('pharma');
    expect(component.filteredSuppliers().length).toBe(1);
    expect(component.filteredSuppliers()[0].name).toBe('PharmaCo');
  });

  // ── selectSupplier ────────────────────────────────────────────────────────

  describe('selectSupplier()', () => {
    it('loads orders and stats for selected supplier', () => {
      const orders: SupplyOrder[] = [{ orderId: 1, medicationId: 10, orderedQuantity: 5, status: 'PENDING' }];
      svcMock.getOrdersForSupplier.mockReturnValue(of(orders));
      svcMock.getSupplierStats.mockReturnValue(of(mockStats));

      component.selectSupplier(mockSuppliers[0]);

      expect(component.selectedSupplier).toEqual(mockSuppliers[0]);
      expect(component.orders().length).toBe(1);
      expect(component.supplierStats?.deliveryRate).toBe(80);
    });
  });

  // ── toggleStatus ─────────────────────────────────────────────────────────

  describe('toggleStatus()', () => {
    it('updates supplier isActive in the list', () => {
      const toggled: Supplier = { ...mockSuppliers[0], isActive: false };
      svcMock.toggleSupplierStatus.mockReturnValue(of(toggled));

      component.toggleStatus(mockSuppliers[0]);

      const updated = component.suppliers().find(s => s.supplierId === 1);
      expect(updated?.isActive).toBe(false);
    });

    it('shows success toast on toggle', () => {
      vi.useFakeTimers();
      svcMock.toggleSupplierStatus.mockReturnValue(of({ ...mockSuppliers[0], isActive: false }));

      component.toggleStatus(mockSuppliers[0]);

      expect(component.toast()).toContain('deactivated');
      vi.advanceTimersByTime(3500);
      expect(component.toast()).toBe('');

      vi.useRealTimers();
    });

    it('shows error toast on failure', () => {
      svcMock.toggleSupplierStatus.mockReturnValue(throwError(() => new Error('fail')));

      component.toggleStatus(mockSuppliers[0]);

      expect(component.toastOk()).toBe(false);
      expect(component.toast()).toContain('Toggle failed');
    });
  });

  // ── isOverdue ─────────────────────────────────────────────────────────────

  describe('isOverdue()', () => {
    it('returns true for pending order with past expected date', () => {
      const order: SupplyOrder = {
        orderId: 1, medicationId: 1, orderedQuantity: 5,
        status: 'PENDING', expectedDeliveryDate: '2020-01-01'
      };
      expect(component.isOverdue(order)).toBe(true);
    });

    it('returns false for delivered order even if past expected date', () => {
      const order: SupplyOrder = {
        orderId: 1, medicationId: 1, orderedQuantity: 5,
        status: 'DELIVERED', expectedDeliveryDate: '2020-01-01'
      };
      expect(component.isOverdue(order)).toBe(false);
    });

    it('returns false when no expectedDeliveryDate set', () => {
      const order: SupplyOrder = { orderId: 1, medicationId: 1, orderedQuantity: 5, status: 'PENDING' };
      expect(component.isOverdue(order)).toBe(false);
    });

    it('returns false for future expected date', () => {
      const future = new Date(); future.setFullYear(future.getFullYear() + 1);
      const order: SupplyOrder = {
        orderId: 1, medicationId: 1, orderedQuantity: 5,
        status: 'PENDING',
        expectedDeliveryDate: future.toISOString().split('T')[0]
      };
      expect(component.isOverdue(order)).toBe(false);
    });
  });

  // ── openAdd / openEdit ────────────────────────────────────────────────────

  describe('modals', () => {
    it('openAdd sets editMode=false and clears form', () => {
      component.openAdd();
      expect(component.showSupplierModal).toBe(true);
      expect(component.editMode).toBe(false);
      expect(component.currentSupplier.name).toBe('');
    });

    it('openEdit populates form with supplier data', () => {
      component.openEdit(mockSuppliers[0]);
      expect(component.editMode).toBe(true);
      expect(component.currentSupplier.name).toBe('PharmaCo');
    });
  });

  // ── validateSupplierName ──────────────────────────────────────────────────

  describe('validateSupplierName()', () => {
    it('returns error for empty name', () => {
      expect(component.validateSupplierName('')).toContain('required');
    });

    it('returns error when name contains numbers', () => {
      expect(component.validateSupplierName('Pharma123')).toContain('letters');
    });

    it('returns empty string for valid name', () => {
      expect(component.validateSupplierName('PharmaCo')).toBe('');
    });
  });

  // ── placeOrder validation ─────────────────────────────────────────────────

  describe('validateOrderQty()', () => {
    it('returns error for quantity < 1', () => {
      expect(component.validateOrderQty(0)).toContain('at least 1');
    });

    it('returns error for decimal quantity', () => {
      expect(component.validateOrderQty(1.5)).toContain('whole number');
    });

    it('returns empty string for valid integer', () => {
      expect(component.validateOrderQty(10)).toBe('');
    });
  });
});
