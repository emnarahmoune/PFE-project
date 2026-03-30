import { CanActivateFn, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { keycloakService } from '../services/keycloak-init.service';

export const roleGuard = (requiredRoles: string[]): CanActivateFn => {
  return () => {
    const roles = keycloakService.getRoles();

    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    return requiredRoles.some(role =>
      roles.map(r => r.toLowerCase()).includes(role.toLowerCase())
    );
  };
};