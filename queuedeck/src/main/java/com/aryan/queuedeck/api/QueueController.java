package com.aryan.queuedeck.api;

import com.aryan.queuedeck.queue.Job;
import com.aryan.queuedeck.queue.QueueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * QueueDeck REST API.
 *
 *   GET    /               service info: version + which pod answered (shows load-balancing across replicas)
 *   POST   /api/jobs       enqueue a job -> 201, or 429 when the queue is full
 *   POST   /api/jobs/next  dequeue the next job per the policy -> 200, or 404 when empty
 *   GET    /api/stats      depth, counters and the current dequeue order
 *   DELETE /api/jobs       clear the queue (handy between demo runs)
 */
@RestController
public class QueueController {

    private final QueueService queue;
    private final String version;
    private final String instance;

    public QueueController(QueueService queue, ObjectProvider<BuildProperties> build) {
        this.queue = queue;
        BuildProperties props = build.getIfAvailable();
        this.version = props != null ? props.getVersion() : "dev";
        this.instance = hostname();
    }

    public record JobRequest(@NotBlank String name, @Min(0) @Max(9) Integer priority) {}

    @GetMapping("/")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "queuedeck");
        body.put("version", version);
        body.put("instance", instance);
        body.put("policy", queue.policyName());
        body.put("capacity", queue.capacity());
        return body;
    }

    @PostMapping("/api/jobs")
    public ResponseEntity<Map<String, Object>> enqueue(@Valid @RequestBody JobRequest req) {
        int priority = req.priority() == null ? 0 : req.priority();
        Optional<Job> job = queue.submit(req.name(), priority);
        Map<String, Object> body = new LinkedHashMap<>();
        if (job.isEmpty()) {
            body.put("rejected", true);
            body.put("reason", "queue full");
            body.put("capacity", queue.capacity());
            body.put("instance", instance);
            return ResponseEntity.status(429).body(body);
        }
        body.put("id", job.get().id());
        body.put("name", job.get().name());
        body.put("priority", job.get().priority());
        body.put("depth", queue.size());
        body.put("instance", instance);
        return ResponseEntity.status(201).body(body);
    }

    @PostMapping("/api/jobs/next")
    public ResponseEntity<Map<String, Object>> next() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("instance", instance);
        return queue.next()
                .map(j -> {
                    body.put("empty", false);
                    body.put("id", j.id());
                    body.put("name", j.name());
                    body.put("priority", j.priority());
                    body.put("depth", queue.size());
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> {
                    body.put("empty", true);
                    return ResponseEntity.status(404).body(body);
                });
    }

    @GetMapping("/api/stats")
    public Map<String, Object> stats() {
        Map<String, Object> body = queue.stats();
        body.put("instance", instance);
        return body;
    }

    @DeleteMapping("/api/jobs")
    public ResponseEntity<Void> clear() {
        queue.clear();
        return ResponseEntity.noContent().build();
    }

    private static String hostname() {
        String env = System.getenv("HOSTNAME"); // Kubernetes sets this to the pod name
        if (env != null && !env.isBlank()) {
            return env;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
