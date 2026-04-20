import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from '../../../core/auth/auth-api.service';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { getLandingRouteByRole } from '../../../core/auth/keycloak.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  loading = false;
  errorMessage = '';

  form = {
    identifier: '',
    password: '',
    rememberMe: true
  };

  constructor(
    private authApi: AuthApiService,
    private authStorage: AuthStorageService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  async submit(): Promise<void> {
    this.errorMessage = '';

    const identifier = this.form.identifier.trim();
    const password = this.form.password;

    if (!identifier || !password) {
      this.errorMessage = 'Username/email and password are required.';
      this.cdr.detectChanges();
      return;
    }

    if (this.loading) {
      return;
    }

    this.loading = true;
    try {
      const response = await firstValueFrom(this.authApi.login({
        identifier,
        password
      }));
      this.authStorage.saveSession(response, this.form.rememberMe);
      this.cdr.detectChanges();
      const preservedRedirect = this.authStorage.consumePostLoginRedirect();
      this.router.navigateByUrl(preservedRedirect || getLandingRouteByRole() || response.redirectTo || '/');
    } catch (err: any) {
      this.errorMessage =
        err?.error?.message ||
        'Invalid username/email or password.';
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }
}
