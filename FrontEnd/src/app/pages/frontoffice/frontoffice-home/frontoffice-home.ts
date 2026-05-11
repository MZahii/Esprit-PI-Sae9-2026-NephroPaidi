import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

@Component({
  selector: 'app-frontoffice-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './frontoffice-home.html',
  styleUrl: './frontoffice-home.scss'
})
export class FrontofficeHomeComponent implements OnInit {
  constructor(private authStorage: AuthStorageService) {}

  ngOnInit(): void {
  }

  get user(): any | null {
    return this.authStorage.getUser();
  }

  get displayName(): string {
    if (this.user?.firstName && this.user?.lastName) {
      return `${this.user.firstName} ${this.user.lastName}`;
    }

    return this.user?.username ?? 'Guardian';
  }

  get displayEmail(): string {
    return this.user?.email ?? '';
  }
}
