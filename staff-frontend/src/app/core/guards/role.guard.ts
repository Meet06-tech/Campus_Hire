import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Route guard that checks if the user has one of the required roles.
 * Configure via route data: { roles: ['UTPO'] }
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as string[] | undefined;
  const currentRole = authService.role;

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  if (currentRole && requiredRoles.includes(currentRole)) {
    return true;
  }

  // If authenticated but wrong role, redirect to their correct dashboard
  if (authService.isAuthenticated && currentRole) {
    const redirectPath = currentRole === 'UTPO' ? '/utpo/dashboard' : '/dtpo/dashboard';
    return router.createUrlTree([redirectPath]);
  }

  return router.createUrlTree(['/login']);
};
