package com.example.app.common;

public class Constants {
    
    /* The hostname of keycloak instance */
    public static final String KEYCLOAK_URL = System.getenv().get("KEYCLOAK_URL");

    /* The keycloak realm */
    public static final String REALM = System.getenv().get("KEYCLOAK_REALM");

    /* Client ID of the frontend-app client in keycloak */
    public static final String CLIENT_ID = System.getenv().get("FRONTEND_KEYCLOAK_CLIENT_ID");

    /* Client secret of the frontend-app client in keycloak */
    public static final String CLIENT_SECRET = System.getenv().get("FRONTEND_KEYCLOAK_CLIENT_SECRET");

    /* The redirect URI for client login */
    public static final String REDIRECT_URI = System.getenv().get("FRONTEND_REDIRECT_URI");

    /* The redirect URI after logout */
    public static final String LOGOUT_REDIRECT_URI = System.getenv().get("FRONTEND_LOGOUT_REDIRECT_URI");

    /* Calendar page */
    public static final String CALENDAR_PAGE = "/calendar.html";
    
    /* Main page */
    public static final String LOGIN_PAGE = "/index.html";
    
    /* Error page */
    public static final String UNAUTHORIZED_PAGE = "/unauthorized.html";
    
    /* Protected resource name as defined in Keycloak */
    public static final String KEYCLOAK_RESOURCE = "frontend-app-resource";

    /* The base URL for calendar service REST API */
    public static final String CALENDAR_API_URL = System.getenv().get("CALENDAR_API_URL");
}
