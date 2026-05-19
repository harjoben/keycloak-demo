package com.example.calendar.repository;

import com.example.calendar.config.DatabaseConfig;
import com.example.calendar.model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that interacts with the events repository
 */
public class EventRepository {

    /* The DB storing the calendar events */
    private final DatabaseConfig dbConfig;

    /**
     * Initialize myself
     */
    public EventRepository() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    /**
     * Insert a new event into the database
     * 
     * @param event
     *              The event to be added.
     * 
     * @return
     *         The created event, on success. Throws exception, otherwise
     */
    public void create(Event event) {

        // Form the SQL query
        String sql = "INSERT INTO events " +

                "(title, description, start_time, end_time, location, created_by, created_at) " +

                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (

                Connection conn = dbConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);

        ) {

            // Prepare the SQL query
            pstmt.setString(1, event.getTitle());
            pstmt.setString(2, event.getDescription());
            pstmt.setLong(3, event.getStartTime());
            pstmt.setLong(4, event.getEndTime());
            pstmt.setString(5, event.getLocation());
            pstmt.setString(6, event.getCreatedBy());
            pstmt.setLong(7, System.currentTimeMillis());

            // Execute DB command
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating event failed, no rows affected.");
            }

        } catch (SQLException e) {

            System.out.printf("Error creating event: %s", e);
            throw new RuntimeException("Failed to create event", e);

        }
    }

    /**
     * List all events created by a user
     * 
     * @param username
     *                 The user requesting the list
     * 
     * @return
     *         List of events
     */
    public List<Event> findByUser(String username) {

        // Form the sql query
        String sql = "SELECT * FROM events WHERE created_by = ? ORDER BY start_time DESC";
        List<Event> events = new ArrayList<>();

        try (

                Connection conn = dbConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)

        ) {

            // Prepare the SQL query
            pstmt.setString(1, username);

            // Get the events
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvent(rs));
                }
            }

            // Done
            System.out.printf("Retrieved %d events for user: %s\n", events.size(), username);
            return events;

        } catch (SQLException e) {

            System.out.printf("Error retrieving events for user %s: %s\n", username, e);
            throw new RuntimeException("Failed to retrieve events for user", e);

        }
    }

    /**
     * Find a single event by ID and created by a particular user
     * 
     * @param id
     *                 The id of the event
     * 
     * @param username
     *                 The user who created the event
     * 
     * @return
     *         Event details
     */
    public Event findByIdAndUser(Long id, String username) {

        // The SQL query
        String sql = "SELECT * FROM events WHERE id = ? AND created_by = ?";

        try (

                Connection conn = dbConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)

        ) {

            // Prepare the query
            pstmt.setLong(1, id);
            pstmt.setString(2, username);

            // Get the event
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEvent(rs);
                }
                return null;
            }

        } catch (SQLException e) {

            System.out.printf("Error finding event by ID: %s; error = %s\n", id, e);
            throw new RuntimeException("Failed to find event", e);

        }
    }

    /**
     * Delete a given event created by a specific user.
     * 
     * @param id
     *                 The ID of the event to be delete
     * 
     * @param username
     *                 The name of the user who created the event
     * 
     * @return
     *         True, on successful delete. False, otherwise
     */
    public boolean delete(Long id, String username) {

        // Form the query
        String sql = "DELETE FROM events WHERE id = ? AND created_by = ?";

        try (

                Connection conn = dbConfig.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)

        ) {

            // Prepare the query
            pstmt.setLong(1, id);
            pstmt.setString(2, username);

            // Delete the event
            int affectedRows = pstmt.executeUpdate();
            boolean deleted = affectedRows > 0;

            if (deleted) {
                System.out.printf("Event deleted with ID: %s\n", id);
            } else {
                System.out.printf("No event found with ID: %s\n", id);
            }

            // Done
            return deleted;

        } catch (SQLException e) {

            System.out.printf("Error deleting event with ID: %s; error = %s\n", id, e);
            throw new RuntimeException("Failed to delete event", e);

        }
    }

    /**
     * Helper method to map a SQL resultset into an Event object
     * 
     * @param rs
     *           The resultset to convert
     * 
     * @return
     *         The corresponding event
     * 
     * @throws SQLException
     */
    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {

        Event event = new Event();

        event.setId(rs.getLong("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setStartTime(rs.getLong("start_time"));
        event.setEndTime(rs.getLong("end_time"));
        event.setLocation(rs.getString("location"));
        event.setCreatedBy(rs.getString("created_by"));
        event.setCreatedAt(rs.getLong("created_at"));

        return event;
    }
}
