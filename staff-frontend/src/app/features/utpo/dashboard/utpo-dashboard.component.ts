import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { MeResponse } from '../../../core/models/auth.models';

@Component({
  selector: 'app-utpo-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './utpo-dashboard.component.html',
  styleUrl: './utpo-dashboard.component.scss'
})
export class UtpoDashboardComponent implements OnInit {
  user = signal<MeResponse | null>(null);
  loading = signal(true);

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.me().subscribe({
      next: (me) => {
        this.user.set(me);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.authService.logout();
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
