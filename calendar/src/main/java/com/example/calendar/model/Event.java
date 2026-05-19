package com.example.calendar.model;

/**
 * The Event definition
 */
public class Event {

    /* The ID of the event */
    private Long id;

    /* The event title */
    private String title;

    /* The event description */
    private String description;

    /* Start time in milliseconds */
    private Long startTime;

    /* End time in milliseconds */
    private Long endTime;

    /* Location of the event */
    private String location;

    /* name of the user who created the event */
    private String createdBy;

    /* Creation time in milliseconds */
    private Long createdAt;

    /**
     * Initialize myself
     */
    public Event() {
        // Do nothing
    }

    /**
     * Initialize myself with the required properties
     */
    public Event(Long id, String title, String description, Long startTime,
            Long endTime, String location, String createdBy, Long createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /*
     * GETTER AND SETTER METHODS
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return

        "Event{" +

                "id=" + id +

                ", title='" + title + '\'' +

                ", description='" + description + '\'' +

                ", startTime=" + startTime +

                ", endTime=" + endTime +

                ", location='" + location + '\'' +

                ", createdBy='" + createdBy + '\'' +

                ", createdAt=" + createdAt +

                '}';
    }
}
