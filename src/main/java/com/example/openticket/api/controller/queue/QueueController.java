package com.example.openticket.api.controller.queue;

import com.example.openticket.api.ApiResponse;
import com.example.openticket.api.controller.queue.dto.request.QueueLeaveRequest;
import com.example.openticket.api.service.queue.QueueService;
import com.example.openticket.api.service.queue.dto.response.QueueLeaveResponse;
import com.example.openticket.api.service.queue.dto.response.QueueStatusResponse;
import com.example.openticket.domain.user.User;
import com.example.openticket.global.auth.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/api/v1/queue/events/{eventId}")
    public ApiResponse<QueueStatusResponse> enterQueue(
            @LoginUser User user,
            @PathVariable Long eventId
    ) {
        return ApiResponse.ok(queueService.enterQueue(eventId, user.getId()));
    }

    @GetMapping("/api/v1/queue/events/{eventId}")
    public ApiResponse<QueueStatusResponse> checkStatus(
            @LoginUser User user,
            @PathVariable Long eventId,
            @RequestParam String token
    ) {
        return ApiResponse.ok(queueService.checkStatus(eventId, token));
    }

    @PostMapping("/api/v1/queue/events/{eventId}/leave")
    public ApiResponse<QueueLeaveResponse> leaveQueue(
            @LoginUser User user,
            @PathVariable Long eventId,
            @Valid @RequestBody QueueLeaveRequest request
    ) {
        return ApiResponse.ok(queueService.leaveQueue(eventId, request.queueToken()));
    }
}
