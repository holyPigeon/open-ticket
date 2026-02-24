package com.example.openticket.api.controller.seat;

import com.example.openticket.api.ApiResponse;
import com.example.openticket.api.service.seat.SeatService;
import com.example.openticket.api.service.seat.dto.response.SeatResponse;
import com.example.openticket.global.queue.CheckQueueToken;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @CheckQueueToken(consume = false)
    @GetMapping("/api/v1/events/{eventId}/seats")
    public ApiResponse<List<SeatResponse>> getSeats(@PathVariable Long eventId) {
        return ApiResponse.ok(seatService.getSeats(eventId));
    }
}
