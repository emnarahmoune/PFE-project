import { CanActivateFn } from '@angular/router';
import { keycloakService } from '../services/keycloak-init.service';

export const authGuard: CanActivateFn = async () => {

  // 🔥 attendre que Keycloak soit prêt
  const isLoggedIn = keycloakService.getToken();

  console.log("TOKEN:", isLoggedIn);

  if (isLoggedIn) {
    return true;
  }

  // ❌ sinon login
  await keycloakService.login();
  return false;
};