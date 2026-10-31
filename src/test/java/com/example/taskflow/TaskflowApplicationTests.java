package com.example.taskflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One end-to-end smoke test: starts the real server on a random port and drives
 * it over HTTP. This is the test the pipeline relies on to prove the packaged
 * jar actually boots.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskflowApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createThenReadTaskOverHttp() {
        ResponseEntity<Map> created = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/tasks",
                Map.of("title", "Smoke test", "description", "created by integration test"),
                Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("title", "Smoke test");

        ResponseEntity<Map> fetched = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/tasks/1", Map.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void healthEndpointIsUp() {
        ResponseEntity<String> health = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("UP");
    }
}
