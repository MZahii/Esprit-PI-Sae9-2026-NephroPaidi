import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: string;
  redirectTo: string;
  userId: number;
  keycloakId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  mustChangePassword: boolean;
}

export interface TokenRefreshRequest {
  refreshToken: string;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  login(payload: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/api/auth/login`, payload);
  }

  refresh(payload: TokenRefreshRequest): Observable<TokenRefreshResponse> {
    return this.http.post<TokenRefreshResponse>(`${this.baseUrl}/api/auth/refresh`, payload);
  }
}
