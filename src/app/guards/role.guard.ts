import { CanActivateFn } from '@angular/router';
import { keycloakService } from '../services/keycloak-init.service';

export const roleGuard = (role: string): CanActivateFn => {
  return () => {
    const roles = keycloakService.getRoles();

    if (roles.includes(role)) return true;

    alert('Accès refusé');
    return false;
  };
};