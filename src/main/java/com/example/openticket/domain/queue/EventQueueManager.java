package com.example.openticket.domain.queue;

public interface EventQueueManager {

    QueueStatus enter(Long eventId, Long userId);

    QueueStatus check(Long eventId, String token);

    boolean validate(Long eventId, String token);

    boolean consumeActiveToken(Long eventId, String token);

    boolean leave(Long eventId, String token);

    void promoteForEvent(Long eventId);
}
