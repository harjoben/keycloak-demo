package com.example.calendar.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class holding the database connection info
 */
public class DatabaseConfig {

    /* The connection string of the events DB */
    private static final String DB_URL = "jdbc:sqlite:/app/data/calendar.db";

    /* My instance */
    private static DatabaseConfig instance;

    /**
     * Initialize myself
     */
    private DatabaseConfig() {
        initializeDatabase();
    }

    /**
     * Get singleton instance
     * 
     * @return
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Initialize the database.
     * 
     * This creates the required events table on the fly.
     */
    private void initializeDatabase() {

        try {

            Class.forName("org.sqlite.JDBC");
            createTables();
            System.out.println("Database initialized successfully");

        } catch (ClassNotFoundException e) {

            System.out.printf("ERROR: SQLite JDBC driver not found : %s\n", e);
            throw new RuntimeException("Failed to load SQLite driver", e);

        }
    }

    /**
     * Create the necessary events table, if not already exists
     */
    private void createTables() {

        // The SQL query to execute
        String createEventsTable =

                "CREATE TABLE IF NOT EXISTS events (" +

                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        "title TEXT NOT NULL, " +

                        "description TEXT, " +

                        "start_time INTEGER NOT NULL, " +

                        "end_time INTEGER NOT NULL, " +

                        "location TEXT, " +

                        "created_by TEXT NOT NULL, " +

                        "created_at INTEGER NOT NULL" +

                        ")";

        // Execute the statement
        try (

                Connection conn = getConnection();
                Statement stmt = conn.createStatement()

        ) {

            stmt.execute(createEventsTable);
            System.out.println("Events table created or already exists");

        } catch (SQLException e) {

            System.out.printf("Error creating tables", e);
            throw new RuntimeException("Failed to create database tables", e);

        }
    }

    /**
     * Get the database connection
     * 
     * @return
     *         DB connection
     * 
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Close the database connection
     * 
     * @param conn
     *             The connection to be closed
     */
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.out.printf("Error closing connection", e);
            }
        }
    }
}
