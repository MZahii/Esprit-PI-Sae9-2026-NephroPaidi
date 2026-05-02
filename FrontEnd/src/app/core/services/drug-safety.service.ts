import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, timeout } from 'rxjs';

export type DrugSafetyLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'UNKNOWN';

export interface DrugSafetySignal {
  queriedDrug: string;
  level: DrugSafetyLevel;
  boxedWarning: boolean;
  warningCount: number;
  adverseReactionCount: number;
}

interface OpenFdaLabelResult {
  boxed_warning?: string[];
  warnings?: string[];
  adverse_reactions?: string[];
}

interface OpenFdaResponse {
  results?: OpenFdaLabelResult[];
}

@Injectable({ providedIn: 'root' })
export class DrugSafetyService {
  private readonly baseUrl = 'https://api.fda.gov/drug/label.json';
  private readonly requestTimeoutMs = 7000;

  constructor(private http: HttpClient) {}

  getSignal(drugName: string): Observable<DrugSafetySignal> {
    const cleaned = drugName.trim();
    const params = new HttpParams()
      .set('search', `openfda.generic_name:"${cleaned}"`)
      .set('limit', '1');

    return this.http.get<OpenFdaResponse>(this.baseUrl, { params }).pipe(
      timeout({ first: this.requestTimeoutMs }),
      map((response) => this.toSignal(cleaned, response.results?.[0]))
    );
  }

  private toSignal(drugName: string, result?: OpenFdaLabelResult): DrugSafetySignal {
    const boxedWarning = (result?.boxed_warning?.length ?? 0) > 0;
    const warningCount = result?.warnings?.length ?? 0;
    const adverseReactionCount = result?.adverse_reactions?.length ?? 0;

    let level: DrugSafetyLevel = 'LOW';
    if (boxedWarning) {
      level = 'HIGH';
    } else if (warningCount >= 3 || adverseReactionCount >= 8) {
      level = 'MODERATE';
    } else if (!result) {
      level = 'UNKNOWN';
    }

    return {
      queriedDrug: drugName,
      level,
      boxedWarning,
      warningCount,
      adverseReactionCount
    };
  }
}
