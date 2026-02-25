package com.example.openticket.domain.queue;

public record QueueStatus(String token, QueuePhase phase, int position, long remainingSeconds) {

    public static QueueStatus waiting(String token, int position) {
        return new QueueStatus(token, QueuePhase.WAITING, position, 0);
    }

    public static QueueStatus allowed(String token, long remainingSeconds) {
        return new QueueStatus(token, QueuePhase.ALLOWED, 0, remainingSeconds);
    }
}
