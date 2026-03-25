// core/guards/role.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { KeycloakInitService } from '../services/keycloak-init.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const keycloakService = inject(KeycloakInitService);
  
  const requiredRoles = route.data['roles'] as string[] || [];
  const userRoles = keycloakService.getUserRoles();
  
  return requiredRoles.some(role => userRoles.includes(role));
};
