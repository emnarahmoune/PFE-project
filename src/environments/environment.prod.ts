export const environment = {
  production: true,
  apiUrl: '/api',
  appName: 'Portail RH',
  version: '1.0.0',
  
  keycloak: {
    url: 'https://keycloak.votre-domaine.com',
    realm: 'portail_rh',
    clientId: 'portail_rh_frontend',
    redirectUri: 'https://votre-domaine.com/admin/dashboard',
    postLogoutRedirectUri: 'https://votre-domaine.com/auth/login',
    sslRequired: 'external',
    publicClient: true,
    enableBearerInterceptor: true,
    bearerExcludedUrls: ['/assets', '/public']
  }
};