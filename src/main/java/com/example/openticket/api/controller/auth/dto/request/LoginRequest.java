package com.example.openticket.api.controller.auth.dto.request;

public record LoginRequest(
        String email,
        String password
) {
}
