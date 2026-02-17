package com.example.openticket.api.controller.event.dto.request;

import com.example.openticket.api.service.event.dto.request.EventSearchServiceRequest;
import com.example.openticket.domain.event.Category;

public record EventSearchRequest(
        String title,
        Category category,
        String venue
) {
    public EventSearchServiceRequest toServiceRequest() {
        return new EventSearchServiceRequest(
                this.title,
                this.category,
                this.venue
        );
    }
}
