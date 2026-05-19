# My Calendar 

## Overview

This is a demonstration of some of Keycloak's IAM capabilities, specifically how a web application and a REST service can be protected using Keycloak.

## The Components

There are three different services in this repo :

### 1. frontend-app

  - This is a web application which lets users login with their Keycloak credentials. 
  - Once authenticated and authorized, users are able to see their calendar events.
  - Only users that are part of the `my-role` role in Keycloak are authorized to access the app.

### 2. calendar

  - This is a REST service that exposes APIs to create, delete and list calendar events for a particular user
  - The APIs are protected by Keycloak. All API requests need to present a valid and active Keycloak-issued access token.
  - Only users that have authenticated with a 2-factor authentication are authorized to access the calendar APIs.

### 3. keycloak

  - The IAM solution protecting the frontend-app and calendar services.
  - The authorization rule to only allow users with role `my-role` to access the frontend-app is defined and enforced in Keycloak.

## The Setup

### Keycloak

1. Start the keycloak server by running this command:

```bash
docker-compose -f keycloak/docker-compose.yml up
```

This will start the server at `http://localhost:8080`

2. The `keycloak/realm-export.json` file will automatically be imported into the running server.
3. Go to Manage -> Clients -> frontend-app -> Credentials, and click on "Regenerate" next to the Client Secret field. Copy the newly generated secret. We will refer to this later as `FRONTEND_CLIENT_SECRET`
4. Similar to Step 3, regenerate the client secret for the 'calendar' client. Remember this as `CALENDAR_CLIENT_SECRET`.
5. Create some test users under Manage -> Users. For testing all the flows, minimally configure different users as:
    - No 2FA and part of 'my-role' role
    - 2FA configured and part of 'my-role' role
    - No 2FA and not part of any roles

### Calendar

1. Copy `calendar/build/docker/.env.template` file to `calendar/build/docker/.env`
2. Set the value of `CALENDAR_KEYCLOAK_CLIENT_SECRET` to what was saved as `CALENDAR_CLIENT_SECRET` above.
3. Run the following command
```bash
docker-compose -f calendar/build/docker/docker-compose.yml up --build
```
The REST service will start on http://localhost:9080

### Frontend App

1. Copy `frontend-app/build/docker/.env.template` file to `frontend-app/build/docker/.env`
2. Set the value of `FRONTEND_KEYCLOAK_CLIENT_SECRET` to what was saved as `FRONTEND_CLIENT_SECRET` above.
3. Run the following command
```bash
docker-compose -f frontend-app/build/docker/docker-compose.yml up --build
```
4. Go to http://localhost:9000 and login using one of the Keycloak users.


## The Flow

The frontend-app implements the standard OIDC Authorization Code Flow for user login:

1. User clicks "Login" button
2. Application redirects to Keycloak authorization endpoint with:
   - client_id
   - redirect_uri
   - response_type=code
   - scope=openid profile
   - state 
3. User authenticates with Keycloak
4. Keycloak redirects back with authorization code
5. Application exchanges code for access token and ID token
6. User is authorized and redirected to the calendar page

For the authorization step in Step 6 above:

1. The application calls Keycloak token endpoint with:
    - grant_type=urn:ietf:params:oauth:grant-type:uma-ticket
    - audience=client_id
    - permission=name-of-protected-resource-on-keycloak
2. The Authorization header for the request is the user's access token retrieved in Step 5 above
3. Keycloak responds with a Requesting Party Token (RPT)
4. A successful RPT generation means that all the enforced authorization policies have passed for the user and the user is allowed to access the resource (/calendar.html in this case)

When the calendar service REST APIs are invoked:

1. The Authorization header is expected to have a Bearer token.
2. The token is sent to Keycloak to be introspected.
3. If the token is valid and active, the `amr` claim in the token is checked.
4. If the `amr` claim has the value `2fa`, it means that the user has authenticated themselves with a second factor. The user is authorized to access the REST API.