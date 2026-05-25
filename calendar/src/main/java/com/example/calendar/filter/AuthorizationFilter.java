package com.example.calendar.filter;

import com.example.calendar.model.ErrorResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;

import javax.net.ssl.SSLContext;

/**
 * Class to authorize the incoming request
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final String KEYCLOAK_URL = System.getenv().get("KEYCLOAK_URL");
    private static final String REALM = System.getenv().get("KEYCLOAK_REALM");
    private static final String CLIENT_ID = System.getenv().get("CALENDAR_KEYCLOAK_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv().get("CALENDAR_KEYCLOAK_CLIENT_SECRET");
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Entrypoint of the filter to authorize the request
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        // Get the authorization header
        String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            String message = "Missing or invalid Authorization header";
            System.out.println(message);
            abort(requestContext, Response.Status.UNAUTHORIZED, message);
            return;
        }

        // Extract token from the value
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {

            // Introspect the token and extract relevant claims
            TokenInfo tokenInfo = introspectToken(token);
            if (tokenInfo == null || !tokenInfo.isActive()) {
                String message = "Token validation failed - token is not active";
                System.out.println(message);
                abort(requestContext, Response.Status.UNAUTHORIZED, message);
                return;
            }

            // Decode the original token to extract AMR claim
            JsonNode tokenClaims = decodeJwtToken(token);
            if (tokenClaims != null && tokenClaims.has("amr")) {
                JsonNode amrNode = tokenClaims.get("amr");
                if (amrNode.isArray()) {
                    for (JsonNode amr : amrNode) {
                        tokenInfo.addAmr(amr.asText());
                    }
                }
            }

            // Check if the user authenticated with a 2FA
            if (!tokenInfo.has2FA()) {
                String message = "2-factor authentication is required to access the resource.";
                System.out.println(message);
                abort(requestContext, Response.Status.FORBIDDEN, message);
                return;
            }

            // Set security context with user information
            final String username = tokenInfo.getUsername();
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> username;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return true;
                }

                @Override
                public boolean isSecure() {
                    return requestContext.getUriInfo().getRequestUri().getScheme().equals("https");
                }

                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });

            // Done
            System.out.printf("User, %s, is authorized to access the calendar REST APIs\n", username);

        } catch (Exception e) {

            String message = "Error validating access token. err = " + e;
            System.out.printf(message);
            abort(requestContext, Response.Status.UNAUTHORIZED, message);

        }
    }

    /**
     * Helper method to introspect the access token with Keycloak and store the
     * relevant claims in TokenInfo object
     * 
     * @param token
     *              The incoming access token to be introspected
     * 
     * @return
     *         The TokenInfo object with the claims from a valid and active access
     *         token
     */
    private TokenInfo introspectToken(String token) {
        try {

            // Use Keycloak's token introspection endpoint
            String introspectUrl = String.format(

                    "%s/realms/%s/protocol/openid-connect/token/introspect",

                    KEYCLOAK_URL,

                    REALM

            );

            try (CloseableHttpClient httpClient = createHttpClient()) {

                HttpPost httpPost = new HttpPost(introspectUrl);

                // Build request body
                String body = String.format(

                        "token=%s&client_id=%s&client_secret=%s",

                        URLEncoder.encode(token, StandardCharsets.UTF_8),

                        URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8),

                        URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8)

                );

                httpPost.setEntity(new StringEntity(body));
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {

                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {

                        // Extract the response body
                        String responseBody = EntityUtils.toString(response.getEntity());
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode introspectionResult = mapper.readTree(responseBody);

                        // Check if token is active
                        if (!introspectionResult.has("active") || !introspectionResult.get("active").asBoolean()) {
                            System.out.println("Token is not active");
                            return null;
                        }

                        // Build TokenInfo from introspection result
                        TokenInfo tokenInfo = new TokenInfo();
                        tokenInfo.setActive(true);
                        tokenInfo.setUsername(introspectionResult.get("username").asText());

                        // Done
                        System.out.printf("Token introspection successful for user: %s\n", tokenInfo.getUsername());
                        return tokenInfo;

                    } else {

                        // We got a non-200 response from introspect endpoint
                        String responseBody = EntityUtils.toString(response.getEntity());
                        System.out.printf("Token introspection failed with status: %s - %s\n", statusCode,
                                responseBody);
                        return null;

                    }
                }
            }
        } catch (Exception e) {
            System.out.printf("Error introspecting token with Keycloak: %s", e);
            return null;
        }
    }

    /**
     * Creates an HTTP client.
     * 
     * We are trusting all self-signed certificates now for demo purposes. A
     * production deployment will have more secure practices in place
     * 
     * @return
     *         CloseableHttpClient
     * 
     * @throws Exception
     */
    private static CloseableHttpClient createHttpClient() throws Exception {

        // Build SSL context
        SSLContext sslContext =

                SSLContextBuilder

                        .create()

                        .loadTrustMaterial(new TrustSelfSignedStrategy())

                        .build();

        // Create SSL socket factory that skips hostname verification
        SSLConnectionSocketFactory sslSocketFactory =

                new SSLConnectionSocketFactory(

                        sslContext,

                        NoopHostnameVerifier.INSTANCE

                );

        // Build and return HTTP client with custom SSL configuration
        return HttpClients

                .custom()

                .setSSLSocketFactory(sslSocketFactory)

                .build();

    }

    /**
     * Helper method to unpack JWT token
     * 
     * @param token
     *              The token to be unpacked
     * 
     * @return
     *         The unpacked token claims as JSON
     * 
     */
    private JsonNode decodeJwtToken(String token) {

        try {

            // JWT tokens have three parts separated by dots: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.out.println("Invalid JWT token format");
                return null;
            }

            // Decode the payload (second part)
            String payload = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode claims = mapper.readTree(decodedPayload);

            // Done
            System.out.printf("Decoded JWT token claims: %s\n", decodedPayload);
            return claims;

        } catch (Exception e) {
            System.out.printf("Error decoding JWT token: %s\n", e);
            return null;
        }
    }

    /**
     * Helper method to properly abort the request with the required status code and
     * message
     * 
     * @param requestContext
     *                       The requestContext to be aborted
     * 
     * @param status
     *                       The desired status code in the response
     * 
     * @param message
     *                       The error message to be displayed in the response
     */
    private void abort(ContainerRequestContext requestContext, Status status, String message) {

        requestContext.abortWith(

                Response

                        .status(status)

                        .entity(new ErrorResponse(message))

                        .build()

        );
    }

    /**
     * Helper class to hold token information
     */
    private static class TokenInfo {

        /* Flag to indicate the status of the token */
        private boolean active;

        /* The name of the user */
        private String username;

        /* The AMR values of the user */
        private java.util.Set<String> amr = new java.util.HashSet<>();

        /*
         * 
         * GETTER AND SETTER METHODS
         * 
         */

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void addAmr(String amrValue) {
            amr.add(amrValue);
        }

        /**
         * Helper method to check whether user authenticated with 2FA
         * 
         * @return
         */
        public boolean has2FA() {
            return amr.contains("2fa");
        }
    }
}
