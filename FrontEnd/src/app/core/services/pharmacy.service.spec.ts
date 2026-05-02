import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PharmacyService } from './pharmacy.service';
import { environment } from '../../../environments/environment';

describe('PharmacyService', () => {
  let svc: PharmacyService;
  let http: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/pharmacy`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PharmacyService]
    });
    svc  = TestBed.inject(PharmacyService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ── Medications ─────────────────────────────────────────────────────────────

  describe('getMedications()', () => {
    it('calls GET /medications and returns array', () => {
      const mock = [{ medicationId: 1, name: 'Amoxicillin', form: 'tablet', pediatricDosage: '10mg' }];

      svc.getMedications().subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].name).toBe('Amoxicillin');
      });

      const req = http.expectOne(`${base}/medications`);
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('createMedication()', () => {
    it('calls POST /medications with body', () => {
      const med = { name: 'Ibuprofen', form: 'syrup', pediatricDosage: '5mg' };
      const created = { ...med, medicationId: 10 };

      svc.createMedication(med as any).subscribe(data => {
        expect(data.medicationId).toBe(10);
      });

      const req = http.expectOne(`${base}/medications`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.name).toBe('Ibuprofen');
      req.flush(created);
    });
  });

  describe('getReorderNeeded()', () => {
    it('calls GET /medications/reorder-needed', () => {
      svc.getReorderNeeded().subscribe();

      const req = http.expectOne(`${base}/medications/reorder-needed`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('sendLowStockAlert()', () => {
    it('calls POST /medications/reorder-needed/send-alert with recipientEmail param', () => {
      svc.sendLowStockAlert('test@hospital.com').subscribe();

      const req = http.expectOne(r =>
        r.url.includes('/medications/reorder-needed/send-alert') &&
        r.params.get('recipientEmail') === 'test@hospital.com'
      );
      expect(req.request.method).toBe('POST');
      req.flush(null);
    });
  });

  // ── Stock ───────────────────────────────────────────────────────────────────

  describe('dispense()', () => {
    it('calls POST /stock/dispense with request body', () => {
      const reqBody = { batchId: 5, quantity: 10 };
      const resp    = { stockId: 1, batchId: 5, quantityAvailable: 40 };

      svc.dispense(reqBody).subscribe(data => {
        expect(data.quantityAvailable).toBe(40);
      });

      const httpReq = http.expectOne(`${base}/stock/dispense`);
      expect(httpReq.request.method).toBe('POST');
      expect(httpReq.request.body.quantity).toBe(10);
      httpReq.flush(resp);
    });
  });

  describe('smartDispense()', () => {
    it('calls POST /stock/smart-dispense', () => {
      svc.smartDispense({ medicationId: 1, quantity: 5 }).subscribe();

      const req = http.expectOne(`${base}/stock/smart-dispense`);
      expect(req.request.method).toBe('POST');
      req.flush({ medicationId: 1, totalDispensed: 5, lines: [] });
    });
  });

  describe('transferStock()', () => {
    it('calls POST /stock/transfer', () => {
      const payload = { sourceBatchId: 1, targetBatchId: 2, quantity: 10, reason: 'test' };

      svc.transferStock(payload).subscribe();

      const req = http.expectOne(`${base}/stock/transfer`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.sourceBatchId).toBe(1);
      req.flush({ sourceBatchId: 1, targetBatchId: 2, quantityTransferred: 10 });
    });
  });

  // ── Suppliers ───────────────────────────────────────────────────────────────

  describe('getSuppliers()', () => {
    it('calls GET /suppliers', () => {
      svc.getSuppliers().subscribe();

      const req = http.expectOne(`${base}/suppliers`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('toggleSupplierStatus()', () => {
    it('calls PATCH /suppliers/{id}/toggle-status', () => {
      svc.toggleSupplierStatus(3).subscribe();

      const req = http.expectOne(`${base}/suppliers/3/toggle-status`);
      expect(req.request.method).toBe('PATCH');
      req.flush({ supplierId: 3, name: 'PharmaCo', isActive: false });
    });
  });

  describe('getSupplierStats()', () => {
    it('calls GET /suppliers/{id}/stats', () => {
      svc.getSupplierStats(3).subscribe(stats => {
        expect(stats.deliveryRate).toBe(75);
      });

      const req = http.expectOne(`${base}/suppliers/3/stats`);
      expect(req.request.method).toBe('GET');
      req.flush({ supplierId: 3, deliveryRate: 75 });
    });
  });

  describe('placeOrder()', () => {
    it('calls POST /suppliers/{supplierId}/orders', () => {
      const order = { medicationId: 1, orderedQuantity: 10 };

      svc.placeOrder(2, order as any).subscribe();

      const req = http.expectOne(`${base}/suppliers/2/orders`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.orderedQuantity).toBe(10);
      req.flush({ orderId: 1, status: 'PENDING' });
    });
  });
});
