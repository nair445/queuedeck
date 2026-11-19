package com.aryan.queuedeck.queue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** First-in, first-out: jobs run in the order they arrived. */
public class FifoJobQueue implements JobQueue {

    private final int capacity;
    private final ArrayDeque<Job> jobs = new ArrayDeque<>();

    public FifoJobQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.capacity = capacity;
    }

    @Override
    public boolean offer(Job job) {
        if (jobs.size() >= capacity) {
            return false;
        }
        jobs.addLast(job);
        return true;
    }

    @Override
    public Optional<Job> poll() {
        return Optional.ofNullable(jobs.pollFirst());
    }

    @Override
    public int size() {
        return jobs.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void clear() {
        jobs.clear();
    }

    @Override
    public List<Job> snapshot() {
        return new ArrayList<>(jobs);
    }

    @Override
    public String policyName() {
        return "FIFO";
    }
}
