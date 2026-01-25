package com.example.openticket.api.service.event.dto.response;

import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        Category category,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String venue
) {
    public static EventResponse of(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getStartAt(),
                event.getEndAt(),
                event.getVenue()
        );
    }
}
