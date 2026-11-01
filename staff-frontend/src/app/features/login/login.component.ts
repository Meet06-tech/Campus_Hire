import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  identifier = '';
  password = '';
  errorMessage = signal<string | null>(null);
  loading = signal(false);

  constructor(private authService: AuthService, private router: Router) {
    // If already authenticated, redirect to dashboard
    if (this.authService.isAuthenticated) {
      this.redirectToDashboard();
    }
  }

  onSubmit(): void {
    if (!this.identifier.trim() || !this.password) {
      this.errorMessage.set('Email and password are required.');
      return;
    }

    this.errorMessage.set(null);
    this.loading.set(true);

    this.authService.login(this.identifier.trim(), this.password).subscribe({
      next: () => {
        this.loading.set(false);
        this.redirectToDashboard();
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401) {
          this.errorMessage.set('Invalid email or password.');
        } else if (err.message === 'This portal is for staff accounts only.') {
          this.errorMessage.set('This portal is for staff accounts only. Students should use the mobile app.');
        } else {
          this.errorMessage.set('Unable to connect to the server. Please try again.');
        }
      }
    });
  }

  private redirectToDashboard(): void {
    const role = this.authService.role;
    if (role === 'UTPO') {
      this.router.navigate(['/utpo/dashboard']);
    } else if (role === 'DTPO') {
      this.router.navigate(['/dtpo/dashboard']);
    }
  }
}
