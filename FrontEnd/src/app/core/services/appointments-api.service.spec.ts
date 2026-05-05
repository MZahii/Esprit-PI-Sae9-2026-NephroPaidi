import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AppointmentsApiService } from './appointments-api.service';
import { environment } from '../../../environments/environment';

describe('AppointmentsApiService', () => {
  let service: AppointmentsApiService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiBaseUrl}/api/appointments`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AppointmentsApiService]
    });

    service = TestBed.inject(AppointmentsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create appointment request with expected payload and endpoint', () => {
    const payload = {
      patientId: 11,
      requestedDate: '2026-05-01T10:00:00',
      reason: 'Follow-up nephrology'
    };

    service.createRequest(payload).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/requests`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({});
  });

  it('should call requests endpoint without status parameter when status is not provided', () => {
    service.getRequests().subscribe();

    const req = httpMock.expectOne((r) => r.url === `${baseUrl}/requests` && !r.params.has('status'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should call requests endpoint with status parameter when status is provided', () => {
    service.getRequests('APPROVED').subscribe();

    const req = httpMock.expectOne((r) => r.url === `${baseUrl}/requests` && r.params.get('status') === 'APPROVED');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should post approval to the appointment request decision endpoint', () => {
    const id = 'a1b2c3';
    const payload = { scheduledDate: '2026-05-03T09:30:00', receptionistNotes: 'validated' };

    service.approve(id, payload).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/requests/${id}/approve`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({});
  });

  it('should post empty body when canceling a request', () => {
    const id = 'cancel-123';

    service.cancel(id).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/requests/${id}/cancel`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({});
  });
});
