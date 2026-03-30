import Keycloak from 'keycloak-js';

class KeycloakService {

  private keycloak!: Keycloak.KeycloakInstance;

  init(): Promise<boolean> {
    this.keycloak = new Keycloak({
      url: 'http://localhost:8080',
      realm: 'rh-platform',
      clientId: 'rh-frontend'
    });

    return this.keycloak.init({
      onLoad: 'check-sso', // ✔ pour afficher frontend
      checkLoginIframe: false
    });
  }

  // ✅ CORRIGÉ ICI
  login(options?: Keycloak.KeycloakLoginOptions): void {
    this.keycloak.login(options);
  }

  logout(): void {
    this.keycloak.logout();
  }

  getToken(): string {
    return this.keycloak.token || '';
  }

  getUsername(): string {
    return this.keycloak.tokenParsed?.['preferred_username'] || '';
  }

getRoles(): string[] {
  const roles = this.keycloak.tokenParsed?.realm_access?.roles || [];

  console.log("ROLES TOKEN:", roles); // 🔥 IMPORTANT

  return roles;
}
  isAdmin(): boolean {
    return this.getRoles().includes('admin');
  }

  isManager(): boolean {
    return this.getRoles().includes('manager');
  }
}

export const keycloakService = new KeycloakService();