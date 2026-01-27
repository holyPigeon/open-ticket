package com.example.openticket.domain.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.example.openticket.IntegrationTestSupport;
import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import com.example.openticket.domain.event.persistence.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SeatRepositoryTest extends IntegrationTestSupport {

    @Autowired SeatRepository seatRepository;
    @Autowired EventRepository eventRepository;

    @DisplayName("좌석 ID 리스트로 좌석들을 조회할 수 있다.")
    @Test
    void findAllById() {
        // given
        Event event = createEvent();
        eventRepository.save(event);

        Seat seat1 = createSeat(event, "A1", 10000);
        Seat seat2 = createSeat(event, "A2", 20000);
        seatRepository.saveAll(List.of(seat1, seat2));

        // when
        List<Seat> seats = seatRepository.findAllById(List.of(seat1.getId(), seat2.getId()));

        // then
        assertThat(seats).hasSize(2)
                .extracting("seatNumber", "price")
                .containsExactlyInAnyOrder(
                        tuple("A1", 10000),
                        tuple("A2", 20000)
                );
    }

    private Seat createSeat(Event event, String seatNumber, int price) {
        return Seat.builder()
                .event(event)
                .seatNumber(seatNumber)
                .price(price)
                .build();
    }

    private Event createEvent() {
        return Event.builder()
                .title("test event 1")
                .category(Category.CONCERT)
                .venue("test venue 1")
                .startAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2027, 1, 1, 0, 0))
                .build();
    }
}