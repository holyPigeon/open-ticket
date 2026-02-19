package com.example.openticket.global.queue;

public record QueueStatus(String token, QueuePhase phase, int position, long remainingSeconds) {

    static QueueStatus waiting(String token, int position) {
        return new QueueStatus(token, QueuePhase.WAITING, position, 0);
    }

    static QueueStatus allowed(String token, long remainingSeconds) {
        return new QueueStatus(token, QueuePhase.ALLOWED, 0, remainingSeconds);
    }
}
