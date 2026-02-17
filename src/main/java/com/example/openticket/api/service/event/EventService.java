package com.example.openticket.api.service.event;

import com.example.openticket.api.service.event.dto.request.EventSearchServiceRequest;
import com.example.openticket.api.service.event.dto.response.EventResponse;
import com.example.openticket.domain.event.persistence.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    public Page<EventResponse> searchEvents(EventSearchServiceRequest request, Pageable pageable) {
        return eventRepository.findAllWithSearchCondition(request.toSearchCondition(), pageable)
                .map(EventResponse::from);
    }

    public EventResponse getEventDetails(Long eventId) {
        return eventRepository.findById(eventId)
                .map(EventResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
    }
}
