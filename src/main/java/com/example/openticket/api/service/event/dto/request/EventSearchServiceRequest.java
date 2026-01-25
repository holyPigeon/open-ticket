package com.example.openticket.api.service.event.dto.request;

import com.example.openticket.domain.event.Category;

public record EventSearchServiceRequest(
        String title,
        Category category,
        String venue
) {

}
