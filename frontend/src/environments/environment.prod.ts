export const environment = {
  production: true,
  apiUrl: '/api',  // Use relative URL for production (proxied by Nginx)
  keycloak: {
    url: window.location.origin.replace('app.', 'auth.'),  // Dynamic KeyCloak URL
    realm: 'personal-finance',
    clientId: 'personal-finance-app'
  }
};
