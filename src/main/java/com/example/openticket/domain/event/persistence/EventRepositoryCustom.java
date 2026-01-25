package com.example.openticket.domain.event.persistence;

import com.example.openticket.api.service.event.dto.request.EventSearchServiceRequest;
import com.example.openticket.domain.event.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRepositoryCustom {

    Page<Event> findAllWithSearchCondition(EventSearchServiceRequest request, Pageable pageable);
}
