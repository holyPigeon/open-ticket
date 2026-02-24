package com.example.openticket.global.queue;

public record ActiveToken(
        Long userId,
        long expiresAt
) {
}
