package com.example.openticket.api.controller.queue.dto.request;

import jakarta.validation.constraints.NotBlank;

public record QueueLeaveRequest(
        @NotBlank(message = "queueToken은 필수입니다.") String queueToken
) {
}
