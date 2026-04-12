import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { KeycloakInitService } from '../services/keycloak-init.service';

export const roleGuard: CanActivateFn = async (
  route: ActivatedRouteSnapshot, 
  state: RouterStateSnapshot
) => {
  const keycloakService = inject(KeycloakInitService);
  const router = inject(Router);
  
  const requiredRoles = route.data['roles'] as string[] || [];
  const userRoles = keycloakService.getUserRoles();
  
  console.log('🔐 roleGuard - Rôles requis:', requiredRoles);
  console.log('👤 Rôles utilisateur:', userRoles);
  
  // Si aucun rôle requis, accès autorisé
  if (requiredRoles.length === 0) {
    return true;
  }
  
  // Vérification insensible à la casse
  const hasRequiredRole = requiredRoles.some((requiredRole: string) => 
    userRoles.some((userRole: string) => userRole.toLowerCase() === requiredRole.toLowerCase())
  );
  
  if (hasRequiredRole) {
    console.log('✅ Rôle autorisé');
    return true;
  }
  
  console.warn('⛔ Accès refusé - Rôle requis non trouvé');
  
  // Redirection selon le rôle de l'utilisateur
  if (userRoles.some(role => role.toLowerCase() === 'manager')) {
    console.log('🔄 Redirection vers /manager/dashboard');
    router.navigate(['/manager/dashboard']);
  } else if (userRoles.some(role => role.toLowerCase() === 'admin')) {
    console.log('🔄 Redirection vers /admin/dashboard');
    router.navigate(['/admin/dashboard']);
  } else if (userRoles.some(role => role.toLowerCase() === 'user')) {
    console.log('🔄 Redirection vers /employee/dashboard');
    router.navigate(['/employee/dashboard']);
  } else {
    console.log('🔄 Redirection vers /auth/login');
    router.navigate(['/auth/login']);
  }
  
  return false;
};