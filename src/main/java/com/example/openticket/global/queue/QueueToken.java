package com.example.openticket.global.queue;

public record QueueToken(
        Long userId,
        long expiresAt
) {
}
