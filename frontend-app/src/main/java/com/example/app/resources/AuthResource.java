package com.example.app.resources;

import com.example.app.common.Constants;
import com.example.app.util.HttpClientUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The resource that defines all auth related endpoints
 */
@Path("/auth")
public class AuthResource {

    /* The scope to use when requesting for auth code */
    private static final String SCOPE = "openid profile";

    /* The request instance */
    @Context
    private HttpServletRequest request;

    /**
     * The initial login endpoint that redirects to the /auth endpoint of Keycloak
     * with response_type=code
     * 
     * @return
     */
    @GET
    @Path("/login")
    public Response login() {

        HttpSession session = request.getSession(true);

        // Generate state parameter for CSRF protection
        String state = generateRandomString(32);
        session.setAttribute("oauth_state", state);

        // Build authorization URL
        String authUrl = String.format(

                "%s/realms/%s/protocol/openid-connect/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",

                Constants.KEYCLOAK_URL,

                Constants.REALM,

                URLEncoder.encode(Constants.CLIENT_ID, StandardCharsets.UTF_8),

                URLEncoder.encode(Constants.REDIRECT_URI, StandardCharsets.UTF_8),

                URLEncoder.encode(SCOPE, StandardCharsets.UTF_8),

                state

        );

        // Redirect
        return Response.seeOther(URI.create(authUrl)).build();
    }

    /**
     * The callback endpoint that is configured as a redirect URI on the keycloak
     * client
     * 
     * @param code
     *              The generated auth code from keycloak
     * 
     * @param state
     *              The initial state sent to keycloak /auth API
     * 
     * @return
     */
    @GET
    @Path("/callback")
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state) {

        // Validate that we have a session
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Session expired").build();
        }

        // Verify state parameter
        String savedState = (String) session.getAttribute("oauth_state");
        if (savedState == null || !savedState.equals(state)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid state parameter").build();
        }

        // Sanity check on auth code
        if (code == null || code.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Authorization code not received").build();
        }

        try {

            // Exchange authorization code for an access and ID token
            String tokenResponse = exchangeCodeForToken(code);

            JsonObject tokens = new Gson().fromJson(tokenResponse, JsonObject.class);

            // Store tokens in session
            session.setAttribute("access_token", tokens.get("access_token").getAsString());
            session.setAttribute("id_token", tokens.get("id_token").getAsString());
            session.setAttribute("authenticated", true);

            // Redirect to the calendar page
            return Response.seeOther(URI.create("/calendar.html")).build();

        } catch (Exception e) {

            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error exchanging code for token: " + e.getMessage()).build();

        }
    }

    /**
     * The endpoint to logout the current user. This invokes the keycloak's logout
     * endpoint with the ID token hint to clear all sessions on Keycloak side
     * 
     * @return
     */
    @GET
    @Path("/logout")
    public Response logout() {

        HttpSession session = request.getSession(false);
        String idToken = null;

        // Retrieve ID token before invalidating session
        if (session != null) {
            idToken = (String) session.getAttribute("id_token");
            session.invalidate();
        }

        // Build Keycloak logout URL
        String logoutUrl;
        if (idToken != null && !idToken.isEmpty()) {

            // Logout from Keycloak with ID token hint
            logoutUrl = String.format(

                    "%s/realms/%s/protocol/openid-connect/logout?id_token_hint=%s&post_logout_redirect_uri=%s",

                    Constants.KEYCLOAK_URL,

                    Constants.REALM,

                    URLEncoder.encode(idToken, StandardCharsets.UTF_8),

                    URLEncoder.encode(Constants.LOGOUT_REDIRECT_URI, StandardCharsets.UTF_8)

            );

            // Redirect to keycloak logout API. keycloak is configured to redirect back to
            // /index.html after logout
            return Response.seeOther(URI.create(logoutUrl)).build();

        }

        // If we don't have an idToken, the logout on keycloak will mostly fail.
        // Since the session is already invalidated, redirect back to /index.html
        return Response.seeOther(URI.create("/index.html")).build();

    }

    /**
     * Method to call /token endpoint on keycloak using the auth code
     * 
     * @param code
     *             The auth code returned by Keycloak
     * 
     * @return
     *         The response of the /token call
     * 
     * @throws Exception
     */
    private String exchangeCodeForToken(String code) throws Exception {

        // Form the token url
        String tokenUrl = String.format(

                "%s/realms/%s/protocol/openid-connect/token",

                Constants.KEYCLOAK_URL,

                Constants.REALM

        );

        // Use mTLS HTTP client for certificate-bound token requests
        try (CloseableHttpClient httpClient = HttpClientUtil.createMTLSHttpClient()) {

            HttpPost httpPost = new HttpPost(tokenUrl);

            // Build the body
            String body = String.format(

                    "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s",

                    URLEncoder.encode(code, StandardCharsets.UTF_8),

                    URLEncoder.encode(Constants.REDIRECT_URI, StandardCharsets.UTF_8),

                    URLEncoder.encode(Constants.CLIENT_ID, StandardCharsets.UTF_8)

            );

            // Decorate the request
            httpPost.setEntity(new StringEntity(body));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode != 200) {
                    System.err.println("Token exchange failed with status: " + statusCode);
                    System.err.println("Response: " + responseBody);
                }
                
                return responseBody;
            }
        }
    }

    /**
     * Helper method to generate random string
     * 
     * @param length
     *               The length of the generated string
     * 
     * @return
     *         The generated string
     */
    private String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
