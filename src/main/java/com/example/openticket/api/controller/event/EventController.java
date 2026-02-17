package com.example.openticket.api.controller.event;

import com.example.openticket.api.ApiResponse;
import com.example.openticket.api.controller.event.dto.request.EventSearchRequest;
import com.example.openticket.api.service.event.EventService;
import com.example.openticket.api.service.event.dto.response.EventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/api/v1/events")
    public ApiResponse<Page<EventResponse>> searchEvents(
            @ModelAttribute EventSearchRequest request,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(eventService.searchEvents(request.toServiceRequest(), pageable));
    }

    @GetMapping("/api/v1/events/{eventId}")
    public ApiResponse<EventResponse> getEventDetails(
            @PathVariable Long eventId
    ) {
        return ApiResponse.ok(eventService.getEventDetails(eventId));
    }
}
