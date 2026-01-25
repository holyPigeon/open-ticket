package com.example.openticket.api.service.event;

import com.example.openticket.api.service.event.dto.request.EventSearchServiceRequest;
import com.example.openticket.api.service.event.dto.response.EventResponse;
import com.example.openticket.domain.event.persistence.EventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public List<EventResponse> searchEvents(EventSearchServiceRequest request, Pageable pageable) {
        return eventRepository.findAllWithSearchCondition(request, pageable)
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    public EventResponse getEventDetails(Long eventId) {
        return eventRepository.findById(eventId)
                .map(EventResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
    }
}
