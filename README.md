# personal_finance_app

## Authentication & Authorization (Resource Server Mode)
- Frontend authenticates directly with Keycloak (Authorization Code + PKCE).
- Backend is a JWT/OIDC resource server only: expects `Authorization: Bearer <access_token>` on protected routes.
- Realm roles from Keycloak are mapped to Spring authorities (`realm_access.roles -> ROLE_<ROLE>`).
- Access rules:
	- Public: `/actuator/health`, `/actuator/info`, `/q/health/**`.
	- Authenticated: `/api/**` (e.g., `/api/me`).
	- Admin only (realm role `admin`): `/admin/**` and admin-facing APIs such as `/api/users`.
- Login/register/forgot/reset endpoints were removed; no passwords are handled by the backend.

## Current user endpoint
- `GET /api/me` returns `sub`, `preferred_username`, `email`, and mapped roles from the access token.
- On first call, a local `User` record is created (just-in-time) using the token `sub` as `keycloakSubject`.
## Keycloak Configuration
- The Keycloak realm configuration is defined in keycloak/realm/Persfin-realm.json.
- This file is automatically imported on startup if the realm does not exist.
- It includes:
  - Users: alice (password: password)
  - Clients: 
    - persfin-frontend (Angular app)
    - personal-finance-api (Backend resource server)
- If you lose clients or users, run docker compose down -v then docker compose up -d to reset Keycloak and re-import the realm.

