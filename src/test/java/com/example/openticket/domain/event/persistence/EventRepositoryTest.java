package com.example.openticket.domain.event.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import com.example.openticket.IntegrationTestSupport;
import com.example.openticket.api.service.event.dto.request.EventSearchServiceRequest;
import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class EventRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private EventRepository eventRepository;

    @DisplayName("카테고리에 따라 이벤트들을 조회할 수 있다.")
    @Test
    void findAllWithSearchCondition1() {
        // given
        Event event1 = createEvent("test event 1", Category.CONCERT, "test venue 1");
        Event event2 = createEvent("test event 2", Category.SPORTS, "test venue 2");
        Event event3 = createEvent("test event 3", Category.CONCERT, "test venue 3");
        eventRepository.saveAll(List.of(event1, event2, event3));

        EventSearchServiceRequest searchRequest = new EventSearchServiceRequest("", Category.CONCERT, "");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        List<Event> events = eventRepository.findAllWithSearchCondition(searchRequest, pageable).toList();

        // then
        assertThat(events).hasSize(2)
                .extracting("title", "category", "venue")
                .containsExactlyInAnyOrder(
                        tuple("test event 1", Category.CONCERT, "test venue 1"),
                        tuple("test event 3", Category.CONCERT, "test venue 3")
                );
    }

    @DisplayName("제목에 따라 이벤트들을 조회할 수 있다.")
    @Test
    void findAllWithSearchCondition2() {
        // given
        Event event1 = createEvent("test event 1", Category.CONCERT, "test venue 1");
        Event event2 = createEvent("test event 2", Category.SPORTS, "test venue 2");
        Event event3 = createEvent("test event 3", Category.CONCERT, "test venue 3");
        eventRepository.saveAll(List.of(event1, event2, event3));

        EventSearchServiceRequest searchRequest = new EventSearchServiceRequest("test event 1", null, "");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        List<Event> events = eventRepository.findAllWithSearchCondition(searchRequest, pageable).toList();

        // then
        assertThat(events).hasSize(1)
                .extracting("title", "category", "venue")
                .containsExactly(
                        tuple("test event 1", Category.CONCERT, "test venue 1")
                );
    }

    @DisplayName("장소에 따라 이벤트들을 조회할 수 있다.")
    @Test
    void findAllWithSearchCondition3() {
        // given
        Event event1 = createEvent("test event 1", Category.CONCERT, "test venue 1");
        Event event2 = createEvent("test event 2", Category.SPORTS, "test venue 2");
        Event event3 = createEvent("test event 3", Category.CONCERT, "test venue 3");
        eventRepository.saveAll(List.of(event1, event2, event3));

        EventSearchServiceRequest searchRequest = new EventSearchServiceRequest("", null, "test venue 1");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        List<Event> events = eventRepository.findAllWithSearchCondition(searchRequest, pageable).toList();

        // then
        assertThat(events).hasSize(1)
                .extracting("title", "category", "venue")
                .containsExactly(
                        tuple("test event 1", Category.CONCERT, "test venue 1")
                );
    }

    @DisplayName("ID 내림차순으로 정렬하여 이벤트들을 조회할 수 있다.")
    @Test
    void findAllWithSearchCondition4() {
        // given
        Event event1 = createEvent("test event 1", Category.CONCERT, "test venue 1");
        Event event2 = createEvent("test event 2", Category.SPORTS, "test venue 2");
        Event event3 = createEvent("test event 3", Category.CONCERT, "test venue 3");
        eventRepository.saveAll(List.of(event1, event2, event3));

        EventSearchServiceRequest searchRequest = new EventSearchServiceRequest("", Category.CONCERT, "");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

        // when
        List<Event> events = eventRepository.findAllWithSearchCondition(searchRequest, pageable)
                .toList();

        // then
        assertThat(events).hasSize(2)
                .extracting("title", "category", "venue")
                .containsExactly(
                        tuple("test event 3", Category.CONCERT, "test venue 3"),
                        tuple("test event 1", Category.CONCERT, "test venue 1")
                );
    }

    private Event createEvent(String title, Category category, String venue) {
        return Event.builder()
                .title(title)
                .category(category)
                .startAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2027, 1, 1, 0, 0))
                .venue(venue)
                .build();
    }
}