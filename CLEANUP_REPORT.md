# Cleanup Report: Safe Removal of Unused Code

## Endpoint → Flow Inventory

### AuthController Endpoints
1. **POST /api/auth/register**
   - Method: `AuthController.register()`
   - Dependencies: `UserRepository`, `PasswordEncoder`, `KeycloakAdminService`
   - Flow: Saves user locally, calls `KeycloakAdminService.createUser()` which uses `RestTemplate`, `AuthProvider`, `KeycloakTokenService` to create user in Keycloak.

2. **POST /api/auth/login**
   - Method: `AuthController.login()`
   - Dependencies: `KeycloakAuthService`
   - Flow: Calls `KeycloakAuthService.passwordGrant()` which uses `AuthProvider`, `RestTemplate` to get token from Keycloak.

3. **POST /api/auth/forgot**
   - Method: `AuthController.forgot()`
   - Dependencies: `AuthService`
   - Flow: Calls `AuthServiceImpl.forgotPassword()` which uses `UserRepository`, `PasswordResetTokenRepository`, `EmailService`, `PasswordEncoder` to generate reset token and send email.

4. **POST /api/auth/reset**
   - Method: `AuthController.reset()`
   - Dependencies: `AuthService`
   - Flow: Calls `AuthServiceImpl.resetPassword()` which uses `PasswordResetTokenRepository`, `UserRepository`, `PasswordEncoder` to reset password.

5. **GET /api/auth/hello**
   - Method: `AuthController.hello()`
   - Dependencies: None
   - Flow: Returns static string.

6. **GET /api/auth/me**
   - Method: `AuthController.me()`
   - Dependencies: `JwtAuthenticationToken` (Spring Security)
   - Flow: Returns authenticated user info from JWT.

### UserController Endpoints
7. **GET /api/users**
   - Method: `UserController.list()`
   - Dependencies: `UserRepository`
   - Flow: Fetches all users, maps to `UserResponse`.

8. **GET /api/users/{id}**
   - Method: `UserController.get()`
   - Dependencies: `UserRepository`
   - Flow: Fetches user by ID, maps to `UserResponse`.

9. **POST /api/users/{id}/photo**
   - Method: `UserController.uploadPhoto()`
   - Dependencies: `UserRepository`, `StorageService`
   - Flow: Uploads photo, saves URL to user.

## Used Classes/Services
- **Controllers**: `AuthController`, `UserController`
- **Services**: `AuthService` (interface), `AuthServiceImpl`, `KeycloakAdminService`, `KeycloakAuthService`, `KeycloakTokenService`
- **Security**: `AuthProvider`, `SecurityConfig`
- **Config**: `RestTemplateConfig`
- **Domain/Repository**: `User`, `PasswordResetToken`, `UserRepository`, `PasswordResetTokenRepository`
- **Other Beans**: `EmailService`, `StorageService`, `PasswordEncoder` (from `SecurityConfig`)
- **Spring Security**: `KeycloakRolesConverter` is NOT used; `SecurityConfig` has inline converter.

## Candidates for Removal
Based on "Find Usages = 0" and no injection/references:

1. **CustomUserDetailsService.java**
   - Status: **NOT FOUND** - File does not exist in codebase. Possibly already removed or never existed.

2. **JwtProperties.java**
   - Status: **NOT FOUND** - File does not exist in codebase. Possibly already removed or never existed.

3. **KeycloakRolesConverter.java**
   - Status: **NOT FOUND** - File does not exist in codebase. Possibly already removed or never existed.

## Removal Plan
- No files to delete, as candidates are not present.
- No code changes needed.
- Project compiles successfully.

## Validation Checklist
- [x] `mvn clean compile` succeeds
- [x] `mvn test` succeeds
- [ ] All endpoints return expected responses
- [ ] `/api/auth/me` still returns JWT info, not `anonymousUser`
