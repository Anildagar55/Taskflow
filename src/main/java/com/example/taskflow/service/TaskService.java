package com.example.taskflow.service;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.web.dto.TaskRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory task store. Swapping this for a JPA repository later does not
 * change the controller or the tests, which is the point of keeping the
 * persistence detail behind this class.
 */
@Service
public class TaskService {

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);
    private final Clock clock;

    public TaskService() {
        this(Clock.systemUTC());
    }

    TaskService(Clock clock) {
        this.clock = clock;
    }

    public List<Task> findAll(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> status == null || task.status() == status)
                .sorted(Comparator.comparing(Task::id))
                .toList();
    }

    public Task findById(Long id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    public Task create(TaskRequest request) {
        Instant now = clock.instant();
        Long id = sequence.incrementAndGet();
        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();
        Task task = new Task(id, request.title().trim(), request.description(), status, now, now);
        tasks.put(id, task);
        return task;
    }

    public Task update(Long id, TaskRequest request) {
        Task existing = findById(id);
        TaskStatus status = request.status() == null ? existing.status() : request.status();
        Task updated = existing.withDetails(request.title().trim(), request.description(), status, clock.instant());
        tasks.put(id, updated);
        return updated;
    }

    public Task changeStatus(Long id, TaskStatus status) {
        Task updated = findById(id).withStatus(status, clock.instant());
        tasks.put(id, updated);
        return updated;
    }

    public void delete(Long id) {
        if (tasks.remove(id) == null) {
            throw new TaskNotFoundException(id);
        }
    }
}
