package com.example.openticket.api.service.user.dto.response;

import com.example.openticket.domain.user.User;

public record UserResponse(
        Long id,
        String name
) {
    public static UserResponse of(User user) {
        return new UserResponse(
                user.getId(),
                user.getName()
        );
    }
}
