import { Injectable } from '@angular/core';

export interface UtilisateurInfo {
  photoUrl: null;
  id: number;
  email: string;
  nom: string;
  prenom: string;
  role: string;
  typeUtilisateur?: string;
  employeId: number | null;
  matricule?: string;
  departement?: string;
  poste?: string;
  soldeConges?: number;
}

@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly TOKEN_KEY = 'rh_auth_token';
  private readonly USER_KEY = 'rh_current_user';
  private readonly REFRESH_TOKEN_KEY = 'rh_refresh_token';

  constructor() {}

  setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  removeToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  removeRefreshToken(): void {
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  setUser(user: UtilisateurInfo): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  getUser(): UtilisateurInfo | null {
    const userStr = localStorage.getItem(this.USER_KEY);
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch (e) {
        console.error('Erreur parsing user:', e);
        return null;
      }
    }
    return null;
  }

  removeUser(): void {
    localStorage.removeItem(this.USER_KEY);
  }

  clear(): void {
    this.removeToken();
    this.removeRefreshToken();
    this.removeUser();
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;

    try {
      const payload = this.decodeToken(token);
      const currentTime = Math.floor(Date.now() / 1000);
      return payload.exp > currentTime;
    } catch (error) {
      return false;
    }
  }

  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;

    try {
      const payload = this.decodeToken(token);
      const currentTime = Math.floor(Date.now() / 1000);
      return payload.exp <= currentTime;
    } catch (error) {
      return true;
    }
  }

  private decodeToken(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (error) {
      throw new Error('Invalid token');
    }
  }

  getTokenExpirationDate(): Date | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      const payload = this.decodeToken(token);
      return new Date(payload.exp * 1000);
    } catch (error) {
      return null;
    }
  }
}