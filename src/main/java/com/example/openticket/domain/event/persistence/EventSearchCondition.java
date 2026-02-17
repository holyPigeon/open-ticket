package com.example.openticket.domain.event.persistence;

import com.example.openticket.domain.event.Category;

public record EventSearchCondition(
        String title,
        Category category,
        String venue
) {

}
