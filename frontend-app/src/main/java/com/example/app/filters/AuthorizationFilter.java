package com.example.app.filters;

import com.example.app.common.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Filter to protect resources using Keycloak Authorization Services
 */
public class AuthorizationFilter implements Filter {

    /**
     * The entrypoint for the filter
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        // Only protect the main calendar page that is invoked after authentication
        if (!requestURI.endsWith(Constants.CALENDAR_PAGE)) {
            chain.doFilter(request, response);
            return;
        }

        // Check if user is authenticated
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            System.out.println("User not authenticated, redirecting to login");
            httpResponse.sendRedirect(Constants.LOGIN_PAGE);
            return;
        }

        // Get access token from session
        String accessToken = (String) session.getAttribute("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            System.out.println("No access token found, redirecting to login");
            httpResponse.sendRedirect(Constants.LOGIN_PAGE);
            return;
        }

        // Check authorization with Keycloak using Authorization Services
        try {

            if (!checkAuthorizationWithKeycloak(accessToken, Constants.KEYCLOAK_RESOURCE)) {
                System.out.println(
                        "User not authorized by Keycloak policies for resource: " + Constants.KEYCLOAK_RESOURCE);
                httpResponse.sendRedirect(Constants.UNAUTHORIZED_PAGE);
                return;
            }

        } catch (Exception e) {
            System.err.println("Error checking authorization: " + e.getMessage());
            e.printStackTrace();
            httpResponse.sendRedirect(Constants.UNAUTHORIZED_PAGE);
            return;
        }

        // User is authenticated and authorized by Keycloak policies
        System.out.println("Authorized user. Allowing access to " + requestURI);
        chain.doFilter(request, response);
    }

    /**
     * Invoke the keycloak /token endpoint requesting for an RPT (Requesting Party
     * Token).
     * 
     * This ensures that any permissions defined for the client on keycloak are
     * checked and validated.
     * In our case, we have a resource-based permission defined that says that only
     * users with the role 'my-role' are authorized to access the /calendar
     * resource. That policy only gets enforced when requesting for an RPT.
     * 
     * The request body of the /token will have the following properties:
     * grant_type=urn:ietf:params:oauth:grant-type:uma-ticket
     * audience=<client_id>
     * permission=<name of resource on keycloak>
     * 
     * The authenticated user's access token will be used as a bearer token to
     * authorize the /token call
     * 
     * @param accessToken
     *                    The access token of the authenticated user
     * 
     * @param resource
     *                    The name of the protected resource on keycloak
     * 
     * @return
     *         True, if RPT was successfully returned. False, otherwise
     * 
     * @throws Exception
     */
    private boolean checkAuthorizationWithKeycloak(String accessToken, String resource) throws Exception {

        // Form the token URL
        String tokenUrl = String.format(

                "%s/realms/%s/protocol/openid-connect/token",

                Constants.KEYCLOAK_URL,

                Constants.REALM);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost(tokenUrl);

            // Form the request body
            String body = String.format(

                    "grant_type=urn:ietf:params:oauth:grant-type:uma-ticket&audience=%s&permission=%s",

                    URLEncoder.encode(Constants.CLIENT_ID, StandardCharsets.UTF_8),

                    URLEncoder.encode(resource, StandardCharsets.UTF_8)

            );

            httpPost.setEntity(new StringEntity(body));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Authorization", "Bearer " + accessToken);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    JsonObject result = new Gson().fromJson(responseBody, JsonObject.class);

                    // If the response had an RPT, the user is authorized to access the resource
                    if (result.has("access_token")) {
                        System.out.println("RPT obtained successfully. Authorized user.");
                        return true;
                    }
                }

                // Any other scenario is treated as unauthorized
                System.out
                        .println("User is not authorized to access the resource. Keycloak RPT request returned status: "
                                + statusCode);
                return false;
            }
        }
    }
}
