import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-guardian-messages',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './messages.page.html',
  styleUrl: './messages.page.scss'
})
export class MessagesPage {}
