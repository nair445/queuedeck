package com.aryan.queuedeck.queue;

import java.util.List;
import java.util.Optional;

/**
 * A bounded queue of jobs with a pluggable dequeue order.
 * Implementations are NOT thread-safe; {@link QueueService} serialises access.
 */
public interface JobQueue {

    /** Adds a job. Returns false (backpressure) when the queue is full; nothing is ever dropped silently. */
    boolean offer(Job job);

    /** Removes and returns the next job according to the policy. */
    Optional<Job> poll();

    int size();

    int capacity();

    void clear();

    /** Jobs ordered from "runs next" to "runs last". */
    List<Job> snapshot();

    /** Short policy name, e.g. "FIFO". */
    String policyName();
}
