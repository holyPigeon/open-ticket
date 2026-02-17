package com.example.openticket.api.service.event.dto.request;

import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.persistence.EventSearchCondition;

public record EventSearchServiceRequest(
        String title,
        Category category,
        String venue
) {

    public EventSearchCondition toSearchCondition() {
        return new EventSearchCondition(
                title,
                category,
                venue
        );
    }
}
