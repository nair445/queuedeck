package com.aryan.queuedeck.queue;

import com.aryan.queuedeck.config.QueueProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thread-safe facade over the configured queue, plus enqueue/reject/dequeue metrics. */
@Service
public class QueueService {

    private final JobQueue queue;
    private final Counter enqueued;
    private final Counter rejected;
    private final Counter dequeued;
    private long nextId = 1;

    public QueueService(QueueProperties props, MeterRegistry registry) {
        this.queue = switch (props.policy()) {
            case FIFO -> new FifoJobQueue(props.capacity());
            case LIFO -> new LifoJobQueue(props.capacity());
            case PRIORITY -> new PriorityJobQueue(props.capacity());
        };
        String policy = queue.policyName();
        this.enqueued = Counter.builder("queuedeck.enqueued").tag("policy", policy).register(registry);
        this.rejected = Counter.builder("queuedeck.rejected").tag("policy", policy).register(registry);
        this.dequeued = Counter.builder("queuedeck.dequeued").tag("policy", policy).register(registry);
        Gauge.builder("queuedeck.depth", this, QueueService::size).tag("policy", policy).register(registry);
    }

    /** Returns the stored job, or empty if the queue was full and the job was rejected. */
    public synchronized Optional<Job> submit(String name, int priority) {
        Job job = new Job(nextId, name, priority);
        if (queue.offer(job)) {
            nextId++;
            enqueued.increment();
            return Optional.of(job);
        }
        rejected.increment();
        return Optional.empty();
    }

    public synchronized Optional<Job> next() {
        Optional<Job> job = queue.poll();
        job.ifPresent(j -> dequeued.increment());
        return job;
    }

    public synchronized void clear() {
        queue.clear();
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized Map<String, Object> stats() {
        List<String> order = queue.snapshot().stream().map(j -> j.name() + "(p" + j.priority() + ")").toList();
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("policy", queue.policyName());
        s.put("capacity", queue.capacity());
        s.put("depth", queue.size());
        s.put("enqueued", (long) enqueued.count());
        s.put("rejected", (long) rejected.count());
        s.put("dequeued", (long) dequeued.count());
        s.put("dequeueOrder", order);
        return s;
    }

    public String policyName() {
        return queue.policyName();
    }

    public int capacity() {
        return queue.capacity();
    }
}
