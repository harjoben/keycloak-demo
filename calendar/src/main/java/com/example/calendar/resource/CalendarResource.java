package com.example.calendar.resource;

import com.example.calendar.model.ErrorResponse;
import com.example.calendar.model.Event;
import com.example.calendar.repository.EventRepository;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CalendarResource {

    /* Our events repository */
    private final EventRepository eventRepository;

    /**
     * Initialize
     */
    public CalendarResource() {
        this.eventRepository = new EventRepository();
    }

    /**
     * REST API to create a new event
     * 
     * @param event
     *                        The event object to be created
     * 
     * @param securityContext
     *                        The security context holding the authorized user's
     *                        info
     * 
     * @return
     *         201, on successful create. 4xx/500 otherwise
     */
    @POST
    public Response createEvent(Event event, @Context SecurityContext securityContext) {

        try {

            // Validate the event
            ErrorResponse err = validateEvent(event, securityContext);
            if (err != null) {

                return Response

                        .status(Response.Status.BAD_REQUEST)

                        .entity(err)

                        .build();
            }

            // Set the user who created the event from the security context
            // We know this exists because it was validated above.
            String username = securityContext.getUserPrincipal().getName();
            event.setCreatedBy(username);
            event.setCreatedAt(System.currentTimeMillis());

            // Create the event
            eventRepository.create(event);
            System.out.printf("Event created by user: %s\n", username);

            // Done
            return Response

                    .status(Response.Status.CREATED)

                    .build();

        } catch (Exception e) {

            System.out.printf("Error creating event: %s\n", e);

            return Response

                    .status(Response.Status.INTERNAL_SERVER_ERROR)

                    .entity(new ErrorResponse("Failed to create event: " + e.getMessage()))

                    .build();
        }
    }

    /**
     * REST API to list all existing events.
     * 
     * This will only list events that were created by the user requesting this API
     * 
     * @param securityContext
     *                        The security context holding the authorized user's
     *                        info
     * 
     * @return
     *         200 with event list on successful flows. 4xx/500, otherwise.
     */
    @GET
    public Response listEvents(@Context SecurityContext securityContext) {
        try {

            // Validate that we have a user
            ErrorResponse err = validateUsername(securityContext);
            if (err != null) {

                return Response

                        .status(Response.Status.BAD_REQUEST)

                        .entity(err)

                        .build();
            }

            // extract the username
            String username = securityContext.getUserPrincipal().getName();
            System.out.printf("Listing events for user: %s\n", username);

            // Find events
            java.util.List<Event> events = eventRepository.findByUser(username);

            // Done
            return Response.ok(events).build();

        } catch (Exception e) {

            System.out.printf("Error listing events: %s\n", e);
            return Response

                    .status(Response.Status.INTERNAL_SERVER_ERROR)

                    .entity(new ErrorResponse("Failed to retrieve events: " + e.getMessage()))

                    .build();

        }
    }

    /**
     * Get single event by ID.
     * 
     * @param id
     *                        The id of the event to be returned
     * 
     * @param securityContext
     *                        The security context holding the authorized user's
     *                        info
     * 
     * @return
     *         200 with event details on successful flow. 4xx/500 otherwise.
     * 
     */
    @GET
    @Path("/{id}")
    public Response getEvent(@PathParam("id") Long id, @Context SecurityContext securityContext) {

        try {

            // Validate that we have a user
            ErrorResponse err = validateUsername(securityContext);
            if (err != null) {

                return Response

                        .status(Response.Status.BAD_REQUEST)

                        .entity(err)

                        .build();
            }

            // Extract username
            String username = securityContext.getUserPrincipal().getName();
            System.out.printf("Getting event %s for user: %s\n", id, username);

            // Find the event
            Event event = eventRepository.findByIdAndUser(id, username);
            if (event == null) {

                return Response

                        .status(Response.Status.NOT_FOUND)

                        .entity(new ErrorResponse("Event not found"))

                        .build();

            }

            // Done
            return Response.ok(event).build();

        } catch (Exception e) {

            System.out.printf("Error getting event: %s\n", e);

            return Response

                    .status(Response.Status.INTERNAL_SERVER_ERROR)

                    .entity(new ErrorResponse("Failed to retrieve event: " + e.getMessage()))

                    .build();

        }
    }

    /**
     * Delete an event. A user can only delete events that were created by them
     * 
     * @param id
     *                        The id of the event to be deleted
     * 
     * @param securityContext
     *                        The security context holding the authorized user's
     *                        info
     * 
     * @return
     *         204 on successful delete. 4xx/500 otherwise
     */
    @DELETE
    @Path("/{id}")
    public Response deleteEvent(@PathParam("id") Long id, @Context SecurityContext securityContext) {

        try {

            // Validate that we have a user
            ErrorResponse err = validateUsername(securityContext);
            if (err != null) {

                return Response

                        .status(Response.Status.BAD_REQUEST)

                        .entity(err)

                        .build();
            }

            // Extract the username
            String username = securityContext.getUserPrincipal().getName();
            System.out.printf("Deleting event %s by user: %s\n", id, username);

            // Delete the event
            boolean deleted = eventRepository.delete(id, username);
            if (!deleted) {

                return Response

                        .status(Response.Status.NOT_FOUND)

                        .entity(new ErrorResponse("Event not found"))

                        .build();

            }

            // Done
            return Response.status(Response.Status.NO_CONTENT).build();

        } catch (Exception e) {

            System.out.printf("Error deleting event: %s\n", e);

            return Response

                    .status(Response.Status.INTERNAL_SERVER_ERROR)

                    .entity(new ErrorResponse("Failed to delete event: " + e.getMessage()))

                    .build();

        }
    }

    /**
     * Helper method to perform basic validation check on the incoming event
     * 
     * @param event
     *                        The event to be created
     * 
     * @param securityContext
     *                        The security context holding the authorized user's
     *                        info
     * 
     * @return
     *         Null, on successful validation. ErrorResponse object, otherwise
     */
    private ErrorResponse validateEvent(Event event, @Context SecurityContext securityContext) {

        // Basic sanity checks
        if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
            return new ErrorResponse("Title is required");
        }

        if (event.getStartTime() == null) {
            return new ErrorResponse("Start time is required");
        }

        if (event.getEndTime() == null) {
            return new ErrorResponse("End time is required");
        }

        // Validate that end time is after start time
        if (event.getEndTime() < event.getStartTime()) {
            return new ErrorResponse("End time must be after start time");
        }

        // Done
        return validateUsername(securityContext);
    }

    /**
     * Helper method to validate that we have an authorized user present in the
     * security context
     * 
     * @param securityContext
     *                        The security context holding the authorized user's
     *                        info
     * 
     * @return
     *         Null, on successful validation. ErrorResponse object, otherwise
     */
    private ErrorResponse validateUsername(@Context SecurityContext securityContext) {

        // Check that we have a username defined
        if (securityContext.getUserPrincipal() == null ||

                securityContext.getUserPrincipal().getName() == null ||

                securityContext.getUserPrincipal().getName().trim().isEmpty()) {

            return new ErrorResponse("Missing user information");

        }

        // Done
        return null;
    }

    // Helper classes for responses

    public static class SuccessResponse {
        private String message;

        public SuccessResponse() {
        }

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
