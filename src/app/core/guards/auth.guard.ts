import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakInitService } from '../services/keycloak-init.service';

export const authGuard: CanActivateFn = async (route, state) => {
  const keycloakService = inject(KeycloakInitService);
  const router = inject(Router);
  
  try {
    const isLogged = await keycloakService.isLoggedIn();
    
    if (isLogged) {
      return true;
    }
    
    // Rediriger vers la page de login
    console.log('🔒 Non authentifié');
    router.navigate(['/auth/login']);
    return false;
    
  } catch (error) {
    console.error('AuthGuard error:', error);
    return false;
  }
};