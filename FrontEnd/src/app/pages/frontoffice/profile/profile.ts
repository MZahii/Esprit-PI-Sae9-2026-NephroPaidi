import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

@Component({
  selector: 'app-frontoffice-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class FrontofficeProfileComponent {
  constructor(private authStorage: AuthStorageService) {}

  get user(): any | null {
    return this.authStorage.getUser();
  }
}
