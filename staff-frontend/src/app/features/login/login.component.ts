import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  mode: 'login' | 'request' | 'verify' | 'reset' = 'login';
  identifier = '';
  password = '';
  recoveryEmail = '';
  otp = '';
  newPassword = '';
  confirmPassword = '';
  recoveryToken = '';
  recoveryMessage = signal<string | null>(null);
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

  requestRecovery(): void {
    const email = this.recoveryEmail.trim();
    if (!email || !email.includes('@')) {
      this.errorMessage.set('Enter a valid email address.');
      return;
    }
    this.loading.set(true); this.errorMessage.set(null);
    this.authService.requestForgotPassword(email).subscribe({
      next: response => { this.loading.set(false); this.recoveryMessage.set(response.message); this.mode = 'verify'; },
      error: error => { this.loading.set(false); this.errorMessage.set(this.apiError(error, 'Unable to request a verification code.')); }
    });
  }

  verifyRecovery(): void {
    if (!/^\d{6}$/.test(this.otp)) { this.errorMessage.set('Enter the 6-digit verification code.'); return; }
    this.loading.set(true); this.errorMessage.set(null);
    this.authService.verifyForgotPassword(this.recoveryEmail.trim(), this.otp).subscribe({
      next: response => { this.loading.set(false); this.recoveryToken = response.recoveryToken; this.mode = 'reset'; },
      error: error => { this.loading.set(false); this.errorMessage.set(this.apiError(error, 'The verification code is invalid or expired.')); }
    });
  }

  resetRecovery(): void {
    if (this.newPassword.length < 8) { this.errorMessage.set('Password must be at least 8 characters.'); return; }
    if (this.newPassword !== this.confirmPassword) { this.errorMessage.set('Passwords do not match.'); return; }
    this.loading.set(true); this.errorMessage.set(null);
    this.authService.resetForgotPassword(this.recoveryToken, this.newPassword, this.confirmPassword).subscribe({
      next: () => { this.loading.set(false); this.mode = 'login'; this.password = ''; this.recoveryMessage.set('Password updated. Sign in with your new password.'); },
      error: error => { this.loading.set(false); this.errorMessage.set(this.apiError(error, 'Unable to reset the password.')); }
    });
  }

  backToLogin(): void { this.mode = 'login'; this.errorMessage.set(null); }

  private apiError(error: HttpErrorResponse, fallback: string): string {
    return error?.error?.message || fallback;
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
