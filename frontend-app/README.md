# Frontend App with Keycloak OIDC Authentication

A Java web application using JAX-RS that demonstrates OIDC Authorization Code Flow with Keycloak.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Keycloak running on localhost:8080

## Building and running the Application

Include a `.env` file in `build/docker`. Use `build/docker/.env.template` as the template for the file and check the values for the following properties:

- `KEYCLOAK_URL`
- `KEYCLOAK_REALM`
- `FRONTEND_KEYCLOAK_CLIENT_ID`
- `FRONTEND_KEYCLOAK_CLIENT_SECRET`
- `FRONTEND_REDIRECT_URI`
- `FRONTEND_LOGOUT_REDIRECT_URI`

```bash
docker-compose -f build/docker/docker-compose.yml up --build
```

The application will start on http://localhost:9000



## OIDC Flow

The application implements the standard OIDC Authorization Code Flow:

1. User clicks "Login" button
2. Application redirects to Keycloak authorization endpoint with:
   - client_id
   - redirect_uri
   - response_type=code
   - scope=openid
   - state (for CSRF protection)
3. User authenticates with Keycloak
4. Keycloak redirects back with authorization code
5. Application exchanges code for access token and ID token
6. Tokens are stored in HTTP session
7. User is redirected to the calendar page

## API Endpoints

- `GET /api/auth/login` - Initiates OIDC login flow
- `GET /api/auth/callback` - Handles OAuth callback with authorization code
- `GET /api/auth/logout` - Logs out user and invalidates session
- `GET /api/auth/user` - Returns current user authentication status



