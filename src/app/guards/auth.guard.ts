import { CanActivateFn } from '@angular/router';
import { keycloakService } from '../services/keycloak-init.service';
export const authGuard: CanActivateFn = () => {
  if (!keycloakService.getToken()) {
    keycloakService.login();
    return false;
  }
  return true;
};