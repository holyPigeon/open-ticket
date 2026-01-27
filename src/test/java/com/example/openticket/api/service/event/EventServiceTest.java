package com.example.openticket.api.service.event;

import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.example.openticket.support.IntegrationTestSupport;
import com.example.openticket.api.service.event.dto.request.EventSearchServiceRequest;
import com.example.openticket.api.service.event.dto.response.EventResponse;
import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import com.example.openticket.domain.event.persistence.EventRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class EventServiceTest extends IntegrationTestSupport {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @DisplayName("카테고리 조건으로 이벤트를 검색할 수 있다.")
    @Test
    void searchEventsByCategory() {
        // given
        saveEvent("test event 1", Category.CONCERT, "test venue 1");
        saveEvent("test event 2", Category.SPORTS, "test venue 2");
        saveEvent("test event 3", Category.CONCERT, "test venue 3");

        EventSearchServiceRequest request = new EventSearchServiceRequest("", Category.CONCERT, "");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<EventResponse> result = eventService.searchEvents(request, pageable);

        // then
        assertThat(result.getContent()).hasSize(2)
                .extracting("title", "category", "venue")
                .containsExactlyInAnyOrder(
                        tuple("test event 1", Category.CONCERT, "test venue 1"),
                        tuple("test event 3", Category.CONCERT, "test venue 3")
                );
    }

    @DisplayName("제목 조건으로 이벤트를 검색할 수 있다.")
    @Test
    void searchEventsByTitle() {
        // given
        saveEvent("test event 1", Category.CONCERT, "test venue 1");
        saveEvent("test event 2", Category.SPORTS, "test venue 2");
        saveEvent("test event 3", Category.CONCERT, "test venue 3");

        EventSearchServiceRequest request = new EventSearchServiceRequest("test event 1", null, "");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<EventResponse> result = eventService.searchEvents(request, pageable);

        // then
        assertThat(result.getContent()).hasSize(1)
                .extracting("title", "category", "venue")
                .containsExactly(
                        tuple("test event 1", Category.CONCERT, "test venue 1")
                );
    }

    @DisplayName("장소 조건으로 이벤트를 검색할 수 있다.")
    @Test
    void searchEventsByVenue() {
        // given
        saveEvent("test event 1", Category.CONCERT, "test venue 1");
        saveEvent("test event 2", Category.SPORTS, "test venue 2");
        saveEvent("test event 3", Category.CONCERT, "test venue 3");

        EventSearchServiceRequest request = new EventSearchServiceRequest("", null, "test venue 1");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<EventResponse> result = eventService.searchEvents(request, pageable);

        // then
        assertThat(result.getContent()).hasSize(1)
                .extracting("title", "category", "venue")
                .containsExactly(
                        tuple("test event 1", Category.CONCERT, "test venue 1")
                );
    }

    @DisplayName("이벤트 목록을 ID 내림차순으로 조회한다.")
    @Test
    void searchEventsWithSort() {
        // given
        saveEvent("test event 1", Category.CONCERT, "test venue 1");
        saveEvent("test event 2", Category.SPORTS, "test venue 2");
        saveEvent("test event 3", Category.CONCERT, "test venue 3");

        EventSearchServiceRequest request = new EventSearchServiceRequest("", Category.CONCERT, "");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

        // when
        Page<EventResponse> result = eventService.searchEvents(request, pageable);

        // then
        assertThat(result.getContent()).hasSize(2)
                .extracting("title", "category", "venue")
                .containsExactly(
                        tuple("test event 3", Category.CONCERT, "test venue 3"),
                        tuple("test event 1", Category.CONCERT, "test venue 1")
                );
    }

    @DisplayName("조건에 맞는 이벤트가 없으면 빈 리스트를 반환한다.")
    @Test
    void searchEventsWithNoResult() {
        // given
        saveEvent("test event 1", Category.CONCERT, "test venue 1");
        saveEvent("test event 2", Category.SPORTS, "test venue 2");
        saveEvent("test event 3", Category.CONCERT, "test venue 3");

        EventSearchServiceRequest request = new EventSearchServiceRequest("없는 제목", null, "");

        // when
        Page<EventResponse> result = eventService.searchEvents(request, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).isEmpty();
    }

    private Event saveEvent(String title, Category category, String venue) {
        Event event = Event.builder()
                .title(title)
                .category(category)
                .startAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2027, 1, 1, 0, 0))
                .venue(venue)
                .build();

        return eventRepository.save(event);
    }
}