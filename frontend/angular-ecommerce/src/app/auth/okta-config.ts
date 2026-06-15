/**
 * Okta OIDC configuration (Milestone 3).
 *
 * Replace `issuer` and `clientId` with the values from your Okta application
 * (Okta admin -> Applications -> your SPA -> General). The placeholders below are
 * valid in *shape* so the app still boots without Okta — sign-in only works once
 * these point at a real Okta org. The matching backend property is
 * `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
 */
export const oktaConfig = {
  issuer: 'https://dev-00000000.okta.com/oauth2/default',
  clientId: '0oaplaceholderclientid00',
  redirectUri: window.location.origin + '/login/callback',
  scopes: ['openid', 'profile', 'email'],
  pkce: true,
};
