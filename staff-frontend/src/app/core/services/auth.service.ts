import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthResponse, MeResponse, StaffLoginRequest, StaffRole } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = environment.apiBaseUrl;
  private readonly tokenKey = 'campushire_staff_token';

  private currentUser$ = new BehaviorSubject<AuthResponse | null>(null);

  constructor(private http: HttpClient, private router: Router) {
    // Restore session from storage on app init
    const stored = sessionStorage.getItem(this.tokenKey);
    if (stored) {
      try {
        this.currentUser$.next(JSON.parse(stored));
      } catch {
        sessionStorage.removeItem(this.tokenKey);
      }
    }
  }

  /** Staff login using email identifier. */
  login(identifier: string, password: string): Observable<AuthResponse> {
    const body: StaffLoginRequest = { identifier, password };
    return this.http.post<AuthResponse>(`${this.apiUrl}/api/auth/login`, body).pipe(
      tap(response => {
        // Only allow staff roles
        if (response.role !== 'UTPO' && response.role !== 'DTPO') {
          throw new Error('This portal is for staff accounts only.');
        }
        this.currentUser$.next(response);
        sessionStorage.setItem(this.tokenKey, JSON.stringify(response));
      })
    );
  }

  /** Fetch current user profile from /api/me. */
  me(): Observable<MeResponse> {
    return this.http.get<MeResponse>(`${this.apiUrl}/api/me`);
  }

  /** Log out and redirect to login. */
  logout(): void {
    this.currentUser$.next(null);
    sessionStorage.removeItem(this.tokenKey);
    this.router.navigate(['/login']);
  }

  /** Observable of the current authenticated user. */
  get user$(): Observable<AuthResponse | null> {
    return this.currentUser$.asObservable();
  }

  /** Synchronous access to the current user snapshot. */
  get user(): AuthResponse | null {
    return this.currentUser$.value;
  }

  /** Current JWT token, or null if not logged in. */
  get token(): string | null {
    return this.currentUser$.value?.token ?? null;
  }

  /** Current role, or null if not logged in. */
  get role(): StaffRole | null {
    return this.currentUser$.value?.role ?? null;
  }

  /** Whether the user is currently authenticated. */
  get isAuthenticated(): boolean {
    return this.token !== null;
  }
}
