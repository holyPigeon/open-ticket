package com.example.openticket.global.queue;

public record QueueEntry(
        Long userId,
        String token,
        long sequence,
        long enteredAt
) {
}
