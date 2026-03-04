package com.example.openticket.domain.queue;

public record ActiveToken(
        Long userId,
        long expiresAt
) {
}
