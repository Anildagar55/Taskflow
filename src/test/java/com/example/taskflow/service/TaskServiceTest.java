package com.example.taskflow.service;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.web.dto.TaskRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskServiceTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T10:00:00Z");

    private TaskService newService() {
        return new TaskService(Clock.fixed(FIXED, ZoneOffset.UTC));
    }

    @Test
    void createAssignsSequentialIdsAndDefaultsStatusToTodo() {
        TaskService service = newService();

        Task first = service.create(new TaskRequest("Write pipeline", "GitHub Actions", null));
        Task second = service.create(new TaskRequest("Containerise app", null, null));

        assertThat(first.id()).isEqualTo(1L);
        assertThat(second.id()).isEqualTo(2L);
        assertThat(first.status()).isEqualTo(TaskStatus.TODO);
        assertThat(first.createdAt()).isEqualTo(FIXED);
    }

    @Test
    void findAllFiltersByStatus() {
        TaskService service = newService();
        service.create(new TaskRequest("a", null, TaskStatus.TODO));
        service.create(new TaskRequest("b", null, TaskStatus.DONE));

        assertThat(service.findAll(null)).hasSize(2);
        assertThat(service.findAll(TaskStatus.DONE))
                .extracting(Task::title)
                .containsExactly("b");
    }

    @Test
    void updateKeepsIdAndCreationTimeButRefreshesUpdatedAt() {
        TaskService service = newService();
        Task created = service.create(new TaskRequest("old", "old body", TaskStatus.TODO));

        Task updated = service.update(created.id(), new TaskRequest("new", "new body", TaskStatus.IN_PROGRESS));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.title()).isEqualTo("new");
        assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
    }

    @Test
    void changeStatusUpdatesOnlyTheStatus() {
        TaskService service = newService();
        Task created = service.create(new TaskRequest("task", "body", TaskStatus.TODO));

        Task done = service.changeStatus(created.id(), TaskStatus.DONE);

        assertThat(done.status()).isEqualTo(TaskStatus.DONE);
        assertThat(done.title()).isEqualTo("task");
    }

    @Test
    void deleteRemovesTheTaskAndSecondDeleteFails() {
        TaskService service = newService();
        Task created = service.create(new TaskRequest("task", null, null));

        service.delete(created.id());

        assertThat(service.findAll(null)).isEmpty();
        assertThatThrownBy(() -> service.delete(created.id()))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        TaskService service = newService();

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void findAllIsSortedById() {
        TaskService service = newService();
        service.create(new TaskRequest("a", null, null));
        service.create(new TaskRequest("b", null, null));
        service.create(new TaskRequest("c", null, null));

        List<Task> tasks = service.findAll(null);

        assertThat(tasks).extracting(Task::id).containsExactly(1L, 2L, 3L);
    }
}
