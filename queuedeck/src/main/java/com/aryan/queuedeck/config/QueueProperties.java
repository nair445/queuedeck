package com.aryan.queuedeck.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Queue settings, bound from application.yml, which reads the environment variables
 * QUEUE_POLICY and QUEUE_CAPACITY (supplied by Ansible's env file or the Kubernetes ConfigMap).
 *
 * Validation is deliberate: an invalid value (e.g. QUEUE_CAPACITY=0) makes the app refuse to start.
 * That powers the rollback demo: a bad config rollout never becomes Ready, and
 * `kubectl rollout undo` brings the last good revision back.
 */
@Validated
@ConfigurationProperties(prefix = "queuedeck")
public record QueueProperties(
        @NotNull Policy policy,
        @Min(1) @Max(100_000) int capacity) {

    public enum Policy { FIFO, LIFO, PRIORITY }
}
