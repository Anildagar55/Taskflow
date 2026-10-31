package com.example.taskflow.domain;

import java.time.Instant;

/**
 * Immutable view of a task. The service layer creates a new instance on every
 * change, which keeps the object safe to hand to any thread.
 */
public record Task(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Task withStatus(TaskStatus newStatus, Instant now) {
        return new Task(id, title, description, newStatus, createdAt, now);
    }

    public Task withDetails(String newTitle, String newDescription, TaskStatus newStatus, Instant now) {
        return new Task(id, newTitle, newDescription, newStatus, createdAt, now);
    }
}
