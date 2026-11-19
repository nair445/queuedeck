package com.aryan.queuedeck.queue;

/** A unit of work. Priority runs 0 (lowest) to 9 (highest) and only matters to the PRIORITY policy. */
public record Job(long id, String name, int priority) {}
