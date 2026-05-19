# Frontend App with Keycloak OIDC Authentication

A Java web application that demonstrates OIDC Authorization Code Flow with Keycloak.


## Building and running the Application

Include a `.env` file in `build/docker`. Use `build/docker/.env.template` as the template for the file. Update the properties based on keycloak setup.

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
6. User is redirected to the calendar page

For the authorization step in Step 6 above:

1. The application calls Keycloak token endpoint with:
    - grant_type=urn:ietf:params:oauth:grant-type:uma-ticket
    - audience=client_id
    - permission=name-of-protected-resource-on-keycloak
2. The Authorization header for the request is the user's access token retrieved in Step 5 above
3. Keycloak responds with a Requesting Party Token (RPT)
4. A successful RPT generation means that all the enforced authorization policies have passed for the user and the user is allowed to access the resource (/calendar.html in this case)

## API Endpoints

- `GET /api/auth/login` - Initiates OIDC login flow
- `GET /api/auth/callback` - Handles OAuth callback with authorization code
- `GET /api/auth/logout` - Logs out user and invalidates session
- `GET /api/auth/user` - Returns current user authentication status



