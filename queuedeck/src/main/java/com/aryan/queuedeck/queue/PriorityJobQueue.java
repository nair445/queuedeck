package com.aryan.queuedeck.queue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/** Highest priority first; jobs with the same priority run in arrival order (lower id first). */
public class PriorityJobQueue implements JobQueue {

    private static final Comparator<Job> ORDER =
            Comparator.comparingInt(Job::priority).reversed().thenComparingLong(Job::id);

    private final int capacity;
    private final PriorityQueue<Job> jobs = new PriorityQueue<>(ORDER);

    public PriorityJobQueue(int capacity) {
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
        jobs.add(job);
        return true;
    }

    @Override
    public Optional<Job> poll() {
        return Optional.ofNullable(jobs.poll());
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
        List<Job> order = new ArrayList<>(jobs);
        order.sort(ORDER);
        return order;
    }

    @Override
    public String policyName() {
        return "PRIORITY";
    }
}
