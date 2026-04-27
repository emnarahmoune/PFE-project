export const environment = {
  production: false,
  apiUrl: '/api',
  appName: 'Portail RH',
  version: '1.0.0',
  
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'portail_rh',
    clientId: 'portail_rh_frontend',
    // ← CHANGÉ: redirection dynamique selon le rôle
    redirectUri: 'http://localhost:4200/',
    postLogoutRedirectUri: 'http://localhost:4200/auth/login',
    sslRequired: 'none',
    publicClient: true,
    enableBearerInterceptor: true,
    bearerExcludedUrls: ['/assets', '/public']
  }
};