import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DoseVerificationRequest {
  age_years:           number;
  weight_kg:           number;
  prescribed_dose_mg:  number;
  recommended_dose_mg: number;
  frequency_per_day:   number;
}

export interface DoseVerificationResponse {
  prediction: 'SAFE' | 'OVERDOSE' | 'UNDERDOSE';
  confidence: number;
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/api/ai`;

  verifyPediatricDose(req: DoseVerificationRequest): Observable<DoseVerificationResponse> {
    return this.http.post<DoseVerificationResponse>(`${this.base}/predict/pediatric-dose`, req);
  }
}
