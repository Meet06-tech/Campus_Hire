import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'utpo',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['UTPO'] },
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/utpo/dashboard/utpo-dashboard.component').then(m => m.UtpoDashboardComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  {
    path: 'dtpo',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['DTPO'] },
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dtpo/dashboard/dtpo-dashboard.component').then(m => m.DtpoDashboardComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
