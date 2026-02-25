package com.example.openticket.domain.queue;

public record QueueEntry(
        Long userId,
        String token,
        long sequence,
        long enteredAt
) {
}
