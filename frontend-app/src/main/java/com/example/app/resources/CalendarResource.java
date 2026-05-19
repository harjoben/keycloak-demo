package com.example.app.resources;

import com.example.app.common.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Resource to proxy calendar API calls using the user's access token from
 * session
 */
@Path("/calendar")
public class CalendarResource {

    @Context
    private HttpServletRequest request;

    /**
     * Get all events for a user
     * 
     * @return
     */
    @GET
    @Path("/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents() {

        // Check if user is authenticated
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return Response

                    .status(Response.Status.UNAUTHORIZED)

                    .entity("{\"error\": \"Not authenticated\"}")

                    .build();
        }

        // Get access token from session
        String accessToken = (String) session.getAttribute("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            return Response

                    .status(Response.Status.UNAUTHORIZED)

                    .entity("{\"error\": \"No access token found\"}")

                    .build();
        }

        try {

            // Call calendar service API with user's access token
            String eventsJson = fetchEventsFromCalendarService(accessToken);

            return Response

                    .ok(eventsJson)

                    .header("Content-Type", "application/json")

                    .build();

        } catch (Exception e) {

            System.err.println("Error fetching events: " + e.getMessage());
            e.printStackTrace();

            return Response

                    .status(Response.Status.INTERNAL_SERVER_ERROR)

                    .entity("{\"error\": \"Failed to load events: err = " + e.getMessage()+ "\", \"message\": \"" + e.getMessage() + "\"}")

                    .build();
        }
    }

    /**
     * Create a new calendar event for the user
     * 
     * @param eventJson
     *                  The event to be created
     * 
     * @return
     */
    @POST
    @Path("/events")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEvent(String eventJson) {

        // Check if user is authenticated
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            return Response

                    .status(Response.Status.UNAUTHORIZED)

                    .entity("{\"error\": \"Not authenticated\"}")

                    .build();
        }

        // Get access token from session
        String accessToken = (String) session.getAttribute("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            return Response

                    .status(Response.Status.UNAUTHORIZED)

                    .entity("{\"error\": \"No access token found\"}")

                    .build();
        }

        try {

            JsonObject eventData = new Gson().fromJson(eventJson, JsonObject.class);

            // Sanity Checks
            if (!eventData.has("title") || eventData.get("title").getAsString().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Event title is required\"}").build();
            }

            if (!eventData.has("startTime")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Event start time is required\"}").build();
            }

            if (!eventData.has("endTime")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Event end time is required\"}").build();
            }

            // Call calendar service API to create event
            String createdEventJson = createEventInCalendarService(accessToken, eventJson);

            return Response

                    .status(Response.Status.CREATED)

                    .entity(createdEventJson)

                    .header("Content-Type", "application/json")

                    .build();

        } catch (Exception e) {

            System.err.println("Error creating event: " + e.getMessage());

            return Response

                    .status(Response.Status.BAD_REQUEST)

                    .entity("{\"error\": \"Failed to create event\", \"message\": \"" + e.getMessage() + "\"}")

                    .build();
        }
    }

    /**
     * Get events from Calendar service
     * 
     * @param accessToken
     *                    The authenticated user's access token
     * 
     * @return
     *         JSON response from calendar service, as string
     * 
     * @throws Exception
     */
    private String fetchEventsFromCalendarService(String accessToken) throws Exception {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet(Constants.CALENDAR_API_URL);

            httpGet.setHeader("Authorization", "Bearer " + accessToken);
            httpGet.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("Calendar service GET response status: " + statusCode);

                if (statusCode == 200) {
                    return responseBody;
                } else if (statusCode == 401) {
                    throw new Exception("Unauthorized - Invalid or expired access token");
                } else if (statusCode == 403) {
                    throw new Exception("Forbidden - Access denied by calendar service");
                } else {
                    throw new Exception("Calendar service returned status " + statusCode + ": " + responseBody);
                }
            }
        }
    }

    /**
     * Create an event
     * 
     * @param accessToken
     *                    The authenticated user's access token
     * 
     * @param eventJson
     *                    The event payload to be created
     * 
     * @return
     *         The JSON response from calendar service
     * 
     * @throws Exception
     */
    private String createEventInCalendarService(String accessToken, String eventJson) throws Exception {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost(Constants.CALENDAR_API_URL);

            // Use the user's access token to authenticate with calendar service
            httpPost.setHeader("Authorization", "Bearer " + accessToken);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(eventJson));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("Calendar service POST response status: " + statusCode);

                if (statusCode == 200 || statusCode == 201) {
                    return responseBody;
                } else if (statusCode == 400) {
                    throw new Exception("Bad request - Invalid event data: " + responseBody);
                } else if (statusCode == 401) {
                    throw new Exception("Unauthorized - Invalid or expired access token");
                } else if (statusCode == 403) {
                    throw new Exception("Forbidden - Access denied by calendar service");
                } else {
                    throw new Exception("Calendar service returned status " + statusCode + ": " + responseBody);
                }
            }
        }
    }
}
