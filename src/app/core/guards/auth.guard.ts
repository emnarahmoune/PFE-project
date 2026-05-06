import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = async (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Ne jamais protéger /auth/login
  if (state.url.startsWith('/auth/login')) {
    return true;
  }

  try {
    const isLogged = await authService.isLoggedIn();

    if (isLogged) {
      return true;
    }

    console.log('🔒 Non authentifié → /auth/login');

    router.navigate(['/auth/login'], {
      queryParams: { redirect: state.url }
    });

    return false;

  } catch (error) {
    console.error('AuthGuard error:', error);

    router.navigate(['/auth/login']);
    return false;
  }
};