export const environment = {
  production: false,
  apiUrl: '/api',
  appName: 'Portail RH (Dev)',
  version: '1.0.0-dev',
  
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'portail_rh',
    clientId: 'portail_rh_frontend',
    redirectUri: 'http://localhost:4200',
    postLogoutRedirectUri: 'http://localhost:4200/auth/login',
    sslRequired: 'none',
    publicClient: true,
    enableBearerInterceptor: true,
    bearerExcludedUrls: ['/assets', '/public']
  }
};