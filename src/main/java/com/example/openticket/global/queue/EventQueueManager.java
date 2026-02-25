package com.example.openticket.global.queue;

public interface EventQueueManager {

    QueueEntry enter(Long eventId, Long userId);

    QueueStatus check(Long eventId, String token);

    boolean validate(Long eventId, String token);

    boolean consumeActiveToken(Long eventId, String token);

    boolean leave(Long eventId, String token);

    void promoteForEvent(Long eventId);
}
