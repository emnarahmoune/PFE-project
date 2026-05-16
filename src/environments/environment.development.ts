export const environment = {
  production: false,
  apiUrl: 'https://pfe-project-6txv.onrender.com/api',
  appName: 'Portail RH (Dev)',
  version: '1.0.0-dev',

  keycloak: {
    url: 'https://pfe-project-1-979y.onrender.com/',
    realm: 'portail_rh',
    clientId: 'portail_rh_frontend',
    redirectUri: 'https://front-gules-theta.vercel.app/',
    postLogoutRedirectUri: 'https://front-gules-theta.vercel.app/auth/login',
    sslRequired: 'none',
    publicClient: true,
    enableBearerInterceptor: true,
    bearerExcludedUrls: ['/assets', '/public']
  },

  powerBiAdminUrl: 'https://app.powerbi.com/reportEmbed?reportId=118ea421-f6ba-4646-bc11-64ff3f2dc03f&autoAuth=true&ctid=dbd6664d-4eb9-46eb-99d8-5c43ba153c61',
  powerBiManagerUrl: 'https://app.powerbi.com/reportEmbed?reportId=513e2ec2-4d61-4acd-8511-70578a614b8b&autoAuth=true&ctid=dbd6664d-4eb9-46eb-99d8-5c43ba153c61',
  powerBiEmployeeUrl: 'https://app.powerbi.com/reportEmbed?reportId=a53f4fd7-a1a8-4c60-b805-f7613a67aa96&autoAuth=true&ctid=dbd6664d-4eb9-46eb-99d8-5c43ba153c61'
};