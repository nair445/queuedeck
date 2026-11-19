package com.aryan.queuedeck;

import com.aryan.queuedeck.queue.FifoJobQueue;
import com.aryan.queuedeck.queue.Job;
import com.aryan.queuedeck.queue.LifoJobQueue;
import com.aryan.queuedeck.queue.PriorityJobQueue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit tests for the three queue policies: no Spring context, run in milliseconds. */
class QueuePolicyTest {

    private static Job job(long id, String name, int priority) {
        return new Job(id, name, priority);
    }

    @Test
    void fifoRunsJobsInArrivalOrder() {
        FifoJobQueue q = new FifoJobQueue(3);
        q.offer(job(1, "a", 0));
        q.offer(job(2, "b", 9));
        q.offer(job(3, "c", 5));
        assertEquals("a", q.poll().orElseThrow().name());
        assertEquals("b", q.poll().orElseThrow().name());
        assertEquals("c", q.poll().orElseThrow().name());
        assertEquals(Optional.empty(), q.poll());
    }

    @Test
    void lifoRunsNewestJobFirst() {
        LifoJobQueue q = new LifoJobQueue(3);
        q.offer(job(1, "a", 0));
        q.offer(job(2, "b", 0));
        q.offer(job(3, "c", 0));
        assertEquals(List.of("c", "b", "a"), q.snapshot().stream().map(Job::name).toList());
        assertEquals("c", q.poll().orElseThrow().name());
        assertEquals("b", q.poll().orElseThrow().name());
    }

    @Test
    void priorityRunsHighestFirstAndBreaksTiesByArrival() {
        PriorityJobQueue q = new PriorityJobQueue(5);
        q.offer(job(1, "low", 1));
        q.offer(job(2, "urgent-first", 9));
        q.offer(job(3, "mid", 5));
        q.offer(job(4, "urgent-second", 9));
        assertEquals(List.of("urgent-first", "urgent-second", "mid", "low"),
                q.snapshot().stream().map(Job::name).toList());
        assertEquals("urgent-first", q.poll().orElseThrow().name());
        assertEquals("urgent-second", q.poll().orElseThrow().name());
        assertEquals("mid", q.poll().orElseThrow().name());
    }

    @Test
    void fullQueueRejectsInsteadOfDroppingAndRecoversAfterPoll() {
        FifoJobQueue q = new FifoJobQueue(1);
        assertTrue(q.offer(job(1, "a", 0)));
        assertFalse(q.offer(job(2, "b", 0)));          // backpressure: caller is told, nothing is lost
        assertEquals(1, q.size());
        q.poll();
        assertTrue(q.offer(job(3, "c", 0)));
    }

    @Test
    void zeroCapacityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FifoJobQueue(0));
        assertThrows(IllegalArgumentException.class, () -> new LifoJobQueue(0));
        assertThrows(IllegalArgumentException.class, () -> new PriorityJobQueue(0));
    }
}
