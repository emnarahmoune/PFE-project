// core/guards/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { KeycloakInitService } from '../services/keycloak-init.service';

export const authGuard: CanActivateFn = async (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const keycloakService = inject(KeycloakInitService);
  const router = inject(Router);
  
  try {
    const isLogged = await keycloakService.isLoggedIn();
    
    if (isLogged) {
      return true;
    }
    
    // ✅ ÉVITE BOUCLE : ne redirige que si PAS sur /auth/login
    if (!state.url.startsWith('/auth/login')) {
      const returnUrl = state.url || '/admin/dashboard';
      keycloakService.login(returnUrl);
    }
    
    return false;
  } catch (error) {
    console.error('AuthGuard error:', error);
    return false;
  }
};
