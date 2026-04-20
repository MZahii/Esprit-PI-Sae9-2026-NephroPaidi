import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

interface AccountSettingsResponse {
  id: number;
  keycloakId: string;
  username: string;
  cin: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  sex?: string;
  role: string;
  accountStatus: string;
  enabled: boolean;
  avatarUrl?: string;
  preferredLanguage: string;
  notificationsEnabled: boolean;
  theme: string;
  mustChangePassword: boolean;
}

@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './account-settings.html',
  styleUrl: './account-settings.scss'
})
export class AccountSettingsComponent implements OnInit {
  loading = false;
  savingProfile = false;
  savingPrefs = false;
  savingPassword = false;

  errorMessage = '';
  successMessage = '';

  activeTab: 'profile' | 'security' | 'preferences' = 'profile';
  forcePasswordChange = false;

  settings: AccountSettingsResponse | null = null;

  profileForm = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    avatarUrl: ''
  };

  preferencesForm = {
    preferredLanguage: 'en',
    notificationsEnabled: true,
    theme: 'light'
  };

  securityForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private authStorage: AuthStorageService
  ) {}

  async ngOnInit(): Promise<void> {
    this.forcePasswordChange = this.route.snapshot.queryParamMap.get('forcePasswordChange') === 'true'
      || this.authStorage.isPasswordChangeRequired();

    if (this.forcePasswordChange) {
      this.activeTab = 'security';
    }

    await this.loadSettings();
  }

  setTab(tab: 'profile' | 'security' | 'preferences'): void {
    if (this.forcePasswordChange && tab !== 'security') {
      return;
    }
    this.activeTab = tab;
  }

  async loadSettings(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      const headers = await this.authHeaders();
      const response = await firstValueFrom(
        this.http.get<AccountSettingsResponse>(`${environment.apiBaseUrl}/api/users/me/settings`, { headers })
      );
      this.settings = response;
      this.profileForm = {
        firstName: response.firstName ?? '',
        lastName: response.lastName ?? '',
        email: response.email ?? '',
        phone: response.phone ?? '',
        avatarUrl: response.avatarUrl ?? ''
      };
      this.preferencesForm = {
        preferredLanguage: response.preferredLanguage ?? 'en',
        notificationsEnabled: response.notificationsEnabled,
        theme: response.theme ?? 'light'
      };
      this.applyPreferencesLocally();
      this.authStorage.setPreferences(this.preferencesForm);

      if (!response.mustChangePassword) {
        this.authStorage.setPasswordChangeRequired(false);
        if (this.forcePasswordChange) {
          this.forcePasswordChange = false;
          if (this.activeTab !== 'security') {
            this.activeTab = 'profile';
          }
        }
      }
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to load account settings.';
    } finally {
      this.loading = false;
    }
  }

  async saveProfile(): Promise<void> {
    const profileValidation = this.validateProfile();
    if (profileValidation) {
      this.errorMessage = profileValidation;
      return;
    }

    this.savingProfile = true;
    this.errorMessage = '';
    this.successMessage = '';

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.http.patch(`${environment.apiBaseUrl}/api/users/me/profile`, {
          firstName: this.profileForm.firstName,
          lastName: this.profileForm.lastName,
          email: this.profileForm.email,
          phone: this.profileForm.phone || null,
          avatarUrl: this.profileForm.avatarUrl.trim() || null
        }, { headers })
      );
      this.successMessage = 'Profile updated successfully.';
      await this.loadSettings();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to update profile.';
    } finally {
      this.savingProfile = false;
    }
  }

  async savePreferences(): Promise<void> {
    const preferenceValidation = this.validatePreferences();
    if (preferenceValidation) {
      this.errorMessage = preferenceValidation;
      return;
    }

    this.savingPrefs = true;
    this.errorMessage = '';
    this.successMessage = '';

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.http.patch(`${environment.apiBaseUrl}/api/users/me/preferences`, {
          preferredLanguage: this.preferencesForm.preferredLanguage,
          notificationsEnabled: this.preferencesForm.notificationsEnabled,
          theme: this.preferencesForm.theme
        }, { headers })
      );
      this.successMessage = 'Preferences updated successfully.';
      this.applyPreferencesLocally();
      this.authStorage.setPreferences(this.preferencesForm);
      await this.loadSettings();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to update preferences.';
    } finally {
      this.savingPrefs = false;
    }
  }

  async changePassword(): Promise<void> {
    const passwordValidation = this.validatePassword();
    if (passwordValidation) {
      this.errorMessage = passwordValidation;
      return;
    }

    this.savingPassword = true;
    this.errorMessage = '';
    this.successMessage = '';

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.http.post(`${environment.apiBaseUrl}/api/users/me/change-password`, {
          currentPassword: this.securityForm.currentPassword,
          newPassword: this.securityForm.newPassword,
          confirmPassword: this.securityForm.confirmPassword
        }, { headers })
      );

      this.securityForm = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      };

      this.successMessage = 'Password changed successfully.';
      this.authStorage.setPasswordChangeRequired(false);
      await this.loadSettings();

      if (this.forcePasswordChange) {
        this.forcePasswordChange = false;
        await this.router.navigateByUrl('/backoffice/dashboard');
      }
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to change password.';
    } finally {
      this.savingPassword = false;
    }
  }

  private async authHeaders(): Promise<HttpHeaders> {
    const token = await getValidToken();
    return new HttpHeaders({ Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' });
  }

  private validateProfile(): string | null {
    const firstName = this.profileForm.firstName.trim();
    const lastName = this.profileForm.lastName.trim();
    const email = this.profileForm.email.trim();
    const phone = this.profileForm.phone.trim();

    if (firstName.length < 3) return 'First name must contain at least 3 characters.';
    if (lastName.length < 3) return 'Last name must contain at least 3 characters.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return 'Please enter a valid email address.';
    if (phone && !/^[+]?\d{8,15}$/.test(phone)) return 'Phone must contain only digits (8 to 15), with optional + prefix.';
    return null;
  }

  private validatePreferences(): string | null {
    const langs = ['en', 'fr', 'ar'];
    const themes = ['light', 'dark', 'system'];
    if (!langs.includes(this.preferencesForm.preferredLanguage)) return 'Unsupported language selection.';
    if (!themes.includes(this.preferencesForm.theme)) return 'Unsupported theme selection.';
    return null;
  }

  private validatePassword(): string | null {
    const current = this.securityForm.currentPassword;
    const next = this.securityForm.newPassword;
    const confirm = this.securityForm.confirmPassword;

    if (!current.trim()) return 'Current password is required.';
    if (!next.trim()) return 'New password is required.';
    if (next.length < 10) return 'New password must be at least 10 characters.';
    if (!/[A-Z]/.test(next)) return 'New password must include at least one uppercase letter.';
    if (!/[a-z]/.test(next)) return 'New password must include at least one lowercase letter.';
    if (!/\d/.test(next)) return 'New password must include at least one number.';
    if (!/[^A-Za-z0-9]/.test(next)) return 'New password must include at least one special character.';
    if (/\s/.test(next)) return 'New password cannot contain spaces.';
    if (next !== confirm) return 'New password and confirmation do not match.';
    if (current === next) return 'New password must be different from current password.';
    return null;
  }

  private applyPreferencesLocally(): void {
    const language = this.preferencesForm.preferredLanguage || 'en';
    const theme = this.preferencesForm.theme || 'light';
    document.documentElement.setAttribute('lang', language);
    document.documentElement.setAttribute('data-theme-preference', theme);
    document.body.classList.toggle('np-theme-dark', theme === 'dark');
  }
}
