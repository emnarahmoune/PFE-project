import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { KeycloakInitService } from '../services/keycloak-init.service';

export const roleGuard: CanActivateFn = async (route: ActivatedRouteSnapshot, state) => {
  const keycloakService = inject(KeycloakInitService);
  const router = inject(Router);
  
  const requiredRoles = route.data['roles'] as string[] || [];
  const userRoles = keycloakService.getUserRoles();
  
  console.log('🔐 roleGuard - Rôles requis:', requiredRoles);
  console.log('👤 Rôles utilisateur:', userRoles);
  
  if (requiredRoles.length === 0) {
    return true;
  }
  
  const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));
  
  if (hasRequiredRole) {
    return true;
  }
  
  // Redirection selon le rôle de l'utilisateur
  if (userRoles.includes('admin')) {
    router.navigate(['/admin/dashboard']);
  } else if (userRoles.includes('manager')) {
    router.navigate(['/manager/dashboard']);
  } else if (userRoles.includes('user')) {
    router.navigate(['/employee/dashboard']);
  } else {
    router.navigate(['/auth/login']);
  }
  
  return false;
};