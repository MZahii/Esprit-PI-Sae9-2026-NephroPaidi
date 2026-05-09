import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { StockComponent } from './stock.component';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { Stock, Batch, Medication } from '../../../../core/models/pharmacy.models';

describe('StockComponent', () => {
  let component: StockComponent;
  let fixture: ComponentFixture<StockComponent>;

  const mockStock: Stock[] = [
    { stockId: 1, batchId: 10, quantityAvailable: 50 },
    { stockId: 2, batchId: 11, quantityAvailable: 3  },
    { stockId: 3, batchId: 12, quantityAvailable: 0  },
  ];

  const mockMeds: Medication[] = [
    { medicationId: 1, name: 'Amoxicillin', form: 'tablet', pediatricDosage: '10mg' }
  ];

  const mockBatches: Batch[] = [
    { batchId: 10, batchNumber: 'B-001', expirationDate: '2026-12-31', quantity: 100 }
  ];

  const svcMock = {
    getAllStock:      vi.fn(),
    getLowStock:      vi.fn(),
    getOutOfStock:    vi.fn(),
    getExpiredBatches: vi.fn(),
    getMedications:   vi.fn(),
    getBatches:       vi.fn(),
    dispense:         vi.fn(),
    initializeStock:  vi.fn(),
    adjustStock:      vi.fn(),
    smartDispense:    vi.fn(),
    transferStock:    vi.fn(),
  };

  beforeEach(async () => {
    svcMock.getAllStock.mockReturnValue(of(mockStock));
    svcMock.getLowStock.mockReturnValue(of([mockStock[1]]));
    svcMock.getOutOfStock.mockReturnValue(of([mockStock[2]]));
    svcMock.getExpiredBatches.mockReturnValue(of([]));
    svcMock.getMedications.mockReturnValue(of(mockMeds));
    svcMock.getBatches.mockReturnValue(of(mockBatches));

    await TestBed.configureTestingModule({
      imports: [StockComponent],
      providers: [
        provideRouter([]),
        { provide: PharmacyService, useValue: svcMock }
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(StockComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.clearAllMocks());

  // ── Init ──────────────────────────────────────────────────────────────────

  it('should create and load all stock data on init', () => {
    expect(component).toBeTruthy();
    expect(component.stocks().length).toBe(3);
    expect(svcMock.getAllStock).toHaveBeenCalled();
    expect(svcMock.getLowStock).toHaveBeenCalled();
    expect(svcMock.getOutOfStock).toHaveBeenCalled();
  });

  // ── activeTab / currentList ───────────────────────────────────────────────

  describe('currentList', () => {
    it('returns all stocks on "all" tab', () => {
      component.activeTab.set('all');
      expect(component.currentList.length).toBe(3);
    });

    it('returns lowStock on "low" tab', () => {
      component.activeTab.set('low');
      expect(component.currentList.length).toBe(1);
    });

    it('returns outOfStock on "out" tab', () => {
      component.activeTab.set('out');
      expect(component.currentList.length).toBe(1);
      expect(component.currentList[0].quantityAvailable).toBe(0);
    });
  });

  // ── stockLevel ────────────────────────────────────────────────────────────

  describe('stockLevel()', () => {
    it('returns "out" when quantityAvailable is 0', () => {
      expect(component.stockLevel(mockStock[2])).toBe('out');
    });

    it('returns "low" when quantity is between 1 and 10', () => {
      expect(component.stockLevel(mockStock[1])).toBe('low');
    });

    it('returns "ok" when quantity is above 10', () => {
      expect(component.stockLevel(mockStock[0])).toBe('ok');
    });
  });

  // ── dispense ──────────────────────────────────────────────────────────────

  describe('dispense()', () => {
    beforeEach(() => {
      component.dispenseForm = { batchId: 10, quantity: 5 };
    });

    it('calls svc.dispense and reloads on success', () => {
      svcMock.dispense.mockReturnValue(of({ stockId: 1, batchId: 10, quantityAvailable: 45 }));

      component.dispense();

      expect(svcMock.dispense).toHaveBeenCalled();
      expect(component.showDispense).toBe(false);
      expect(component.toastOk()).toBe(true);
    });

    it('shows validation error for quantity < 1', () => {
      component.dispenseForm.quantity = 0;
      component.dispense();
      expect(component.dispenseError()).toContain('at least 1');
      expect(svcMock.dispense).not.toHaveBeenCalled();
    });

    it('shows validation error for non-integer quantity', () => {
      component.dispenseForm.quantity = 2.5;
      component.dispense();
      expect(component.dispenseError()).toContain('whole number');
    });

    it('shows error toast on service failure', () => {
      svcMock.dispense.mockReturnValue(throwError(() => ({ error: { message: 'Insufficient stock' } })));

      component.dispense();

      expect(component.toastOk()).toBe(false);
      expect(component.toast()).toContain('Insufficient stock');
    });
  });

  // ── adjust ────────────────────────────────────────────────────────────────

  describe('adjust()', () => {
    beforeEach(() => {
      component.adjustBatchId = 10;
      component.adjustDelta   = 5;
      component.adjustReason  = 'Recount';
    });

    it('calls svc.adjustStock on valid input', () => {
      svcMock.adjustStock.mockReturnValue(of({ stockId: 1, batchId: 10, quantityAvailable: 55 }));

      component.adjust();

      expect(svcMock.adjustStock).toHaveBeenCalledWith(10, 5, 'Recount');
      expect(component.showAdjust).toBe(false);
    });

    it('shows error when delta is 0', () => {
      component.adjustDelta = 0;
      component.adjust();
      expect(component.adjustError()).toContain('cannot be zero');
      expect(svcMock.adjustStock).not.toHaveBeenCalled();
    });

    it('shows error when reason is empty', () => {
      component.adjustReason = '  ';
      component.adjust();
      expect(component.adjustError()).toContain('Reason is required');
    });
  });

  // ── initStock ─────────────────────────────────────────────────────────────

  describe('initStock()', () => {
    it('shows error when no batch selected', () => {
      component.initSelectedBatch = null;
      component.initQty = 10;
      component.initStock();
      expect(component.initError()).toContain('select a batch');
    });

    it('shows error when qty exceeds batch size', () => {
      component.initSelectedBatch = { batchId: 10, batchNumber: 'B-001', expirationDate: '2026-01-01', quantity: 50 };
      component.initQty = 100;
      component.initStock();
      expect(component.initError()).toContain('exceed');
    });

    it('calls svc.initializeStock on valid input', () => {
      component.initSelectedBatch = { batchId: 10, batchNumber: 'B-001', expirationDate: '2026-01-01', quantity: 100 };
      component.initQty = 50;
      svcMock.initializeStock.mockReturnValue(of({ stockId: 1, batchId: 10, quantityAvailable: 50 }));

      component.initStock();

      expect(svcMock.initializeStock).toHaveBeenCalledWith(10, 50);
    });
  });

  // ── smartDispense ─────────────────────────────────────────────────────────

  describe('submitSmartDispense()', () => {
    it('shows error when medication not selected', () => {
      component.smartDispenseForm = { quantity: 5 };
      component.submitSmartDispense();
      expect(component.smartDispenseError()).toContain('select a medication');
    });

    it('shows error when quantity < 1', () => {
      component.smartDispenseForm = { medicationId: 1, quantity: 0 };
      component.submitSmartDispense();
      expect(component.smartDispenseError()).toContain('at least 1');
    });

    it('calls svc.smartDispense and stores result', () => {
      const result = { medicationId: 1, medicationName: 'Amoxicillin', requested: 5, totalDispensed: 5, lines: [] };
      svcMock.smartDispense.mockReturnValue(of(result));

      component.smartDispenseForm = { medicationId: 1, quantity: 5 };
      component.submitSmartDispense();

      expect(component.smartDispenseResult?.totalDispensed).toBe(5);
      expect(component.toastOk()).toBe(true);
    });
  });

  // ── sourceAvailableQty ────────────────────────────────────────────────────

  describe('sourceAvailableQty', () => {
    it('returns quantity for selected source batch', () => {
      component.transferForm = { sourceBatchId: 10 };
      expect(component.sourceAvailableQty).toBe(50);
    });

    it('returns 0 when source batch not found', () => {
      component.transferForm = { sourceBatchId: 999 };
      expect(component.sourceAvailableQty).toBe(0);
    });
  });

  // ── toast ─────────────────────────────────────────────────────────────────

  describe('notify()', () => {
    it('sets toast message and clears it after 3.5s', () => {
      vi.useFakeTimers();

      component.notify('Test message', true);
      expect(component.toast()).toBe('Test message');
      expect(component.toastOk()).toBe(true);

      vi.advanceTimersByTime(3500);
      expect(component.toast()).toBe('');

      vi.useRealTimers();
    });
  });
});
