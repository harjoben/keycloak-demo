# Calendar REST Service

A JAX-RS based REST service for managing calendar events with Keycloak authentication and SQLite persistence.

## Features

- **Create Events**: Add new calendar events with title, description, start/end times, and location
- **List Events**: Retrieve all calendar events
- **Get Event**: Retrieve a specific event by ID
- **Delete Events**: Remove events from the calendar
- **Keycloak Authentication**: All endpoints are protected with bearer token authentication
- **SQLite Persistence**: Events are stored in a local SQLite database

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- A Keycloak realm configured with a client for this service


## Configuration

Copy the `build/docker/.env.template` to `build/docker/.env` and update the properties based on the keycloak setup.




## Building and running the Application

Run this command

```bash
docker-compose -f build/docker/docker-compose.yml up --build
```

The service will be available at: `http://localhost:9080/api/events`


## API Endpoints

All endpoints require a valid bearer token issued by Keycloak in the `Authorization` header.

### Create Event

```http
POST /api/events
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "title": "Team Meeting",
  "description": "Weekly team sync",
  "startTime": "2024-01-15T10:00:00",
  "endTime": "2024-01-15T11:00:00",
  "location": "Conference Room A"
}
```

**Response (201 Created):**


### List All Events

```http
GET /api/events
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Team Meeting",
    "description": "Weekly team sync",
    "startTime": "2024-01-15T10:00:00",
    "endTime": "2024-01-15T11:00:00",
    "location": "Conference Room A",
    "createdBy": "john.doe",
    "createdAt": "2024-01-10T14:30:00"
  }
]
```

### Get Event by ID

```http
GET /api/events/{id}
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Team Meeting",
  "description": "Weekly team sync",
  "startTime": "2024-01-15T10:00:00",
  "endTime": "2024-01-15T11:00:00",
  "location": "Conference Room A",
  "createdBy": "john.doe",
  "createdAt": "2024-01-10T14:30:00"
}
```

### Delete Event

```http
DELETE /api/events/{id}
Authorization: Bearer <access_token>
```

**Response (204 No Content):**


## Database

The service uses SQLite for persistence. The database file `calendar.db` is created automatically in the application's working directory.

### Schema

```sql
CREATE TABLE events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    start_time INTEGER NOT NULL,
    end_time INTEGER NOT NULL,
    location TEXT,
    created_by TEXT NOT NULL,
    created_at INTEGER NOT NULL
)
```
