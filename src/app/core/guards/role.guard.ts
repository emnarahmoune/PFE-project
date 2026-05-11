import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router,
  ActivatedRouteSnapshot,
  RouterStateSnapshot
} from '@angular/router';

import { KeycloakInitService } from '../services/keycloak-init.service';

export const roleGuard: CanActivateFn = async (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const keycloakService = inject(KeycloakInitService);
  const router = inject(Router);

  const requiredRoles = (route.data['roles'] as string[]) || [];
  const userRoles = keycloakService.getUserRoles();

  const normalizedUserRoles = userRoles.map(role =>
    String(role).toLowerCase()
  );

  const normalizedRequiredRoles = requiredRoles.map(role =>
    String(role).toLowerCase()
  );

  console.log('🔐 roleGuard - URL:', state.url);
  console.log('🔐 roleGuard - Rôles requis:', requiredRoles);
  console.log('👤 roleGuard - Rôles utilisateur:', userRoles);

  if (requiredRoles.length === 0) {
    return true;
  }

  const hasRequiredRole = normalizedRequiredRoles.some(requiredRole =>
    normalizedUserRoles.includes(requiredRole)
  );

  if (hasRequiredRole) {
    console.log('✅ Rôle autorisé');
    return true;
  }

  console.warn('⛔ Accès refusé - Rôle requis non trouvé');

  if (
    normalizedUserRoles.includes('admin') ||
    normalizedUserRoles.includes('admin_rh') ||
    normalizedUserRoles.includes('rh')
  ) {
    router.navigate(['/admin/dashboard']);
    return false;
  }

  if (normalizedUserRoles.includes('manager')) {
    router.navigate(['/manager/dashboard']);
    return false;
  }

  if (
    normalizedUserRoles.includes('user') ||
    normalizedUserRoles.includes('employe') ||
    normalizedUserRoles.includes('employee')
  ) {
    router.navigate(['/employee/dashboard']);
    return false;
  }

  router.navigate(['/auth/login']);
  return false;
};