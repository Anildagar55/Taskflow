package com.example.taskflow.web;

import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.service.TaskNotFoundException;
import com.example.taskflow.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    private static Task sampleTask() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        return new Task(1L, "Write pipeline", "GitHub Actions", TaskStatus.TODO, now, now);
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        given(taskService.create(any())).willReturn(sampleTask());

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Write pipeline", "description", "GitHub Actions"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/tasks/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void listReturnsTasks() throws Exception {
        given(taskService.findAll(null)).willReturn(List.of(sampleTask()));

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Write pipeline"));
    }

    @Test
    void getReturns404ForUnknownTask() throws Exception {
        given(taskService.findById(9L)).willThrow(new TaskNotFoundException(9L));

        mockMvc.perform(get("/api/v1/tasks/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Task not found"));
    }

    @Test
    void changeStatusReturnsUpdatedTask() throws Exception {
        Task done = sampleTask().withStatus(TaskStatus.DONE, Instant.parse("2026-01-02T10:00:00Z"));
        given(taskService.changeStatus(eq(1L), eq(TaskStatus.DONE))).willReturn(done);

        mockMvc.perform(patch("/api/v1/tasks/1/status").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        willThrow(new TaskNotFoundException(5L)).given(taskService).delete(5L);

        mockMvc.perform(delete("/api/v1/tasks/5"))
                .andExpect(status().isNotFound());
    }
}
