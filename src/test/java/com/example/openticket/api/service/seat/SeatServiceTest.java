package com.example.openticket.api.service.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import com.example.openticket.api.service.seat.dto.response.SeatResponse;
import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import com.example.openticket.domain.event.persistence.EventRepository;
import com.example.openticket.domain.seat.Seat;
import com.example.openticket.domain.seat.SeatRepository;
import com.example.openticket.domain.seat.SeatStatus;
import com.example.openticket.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SeatServiceTest extends IntegrationTestSupport {

    @Autowired
    private SeatService seatService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @DisplayName("이벤트 ID로 좌석 목록을 조회한다.")
    @Test
    void getSeats() {
        // given
        Event event = saveEvent("콘서트 A");
        saveSeat(event, "A1", 10000);
        saveSeat(event, "A2", 15000);
        saveSeat(event, "B1", 20000);

        // when
        List<SeatResponse> result = seatService.getSeats(event.getId());

        // then
        assertThat(result).hasSize(3)
                .extracting("seatNumber", "price", "status")
                .containsExactlyInAnyOrder(
                        tuple("A1", 10000, SeatStatus.AVAILABLE),
                        tuple("A2", 15000, SeatStatus.AVAILABLE),
                        tuple("B1", 20000, SeatStatus.AVAILABLE)
                );
    }

    @DisplayName("이벤트에 좌석이 없으면 빈 리스트를 반환한다.")
    @Test
    void getSeatsEmpty() {
        // given
        Event event = saveEvent("콘서트 B");

        // when
        List<SeatResponse> result = seatService.getSeats(event.getId());

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("다른 이벤트의 좌석은 조회되지 않는다.")
    @Test
    void getSeatsOnlyForTargetEvent() {
        // given
        Event event1 = saveEvent("콘서트 A");
        Event event2 = saveEvent("콘서트 B");
        saveSeat(event1, "A1", 10000);
        saveSeat(event1, "A2", 10000);
        saveSeat(event2, "B1", 20000);

        // when
        List<SeatResponse> result = seatService.getSeats(event1.getId());

        // then
        assertThat(result).hasSize(2)
                .extracting("seatNumber")
                .containsExactlyInAnyOrder("A1", "A2");
    }

    private Event saveEvent(String title) {
        Event event = Event.builder()
                .title(title)
                .category(Category.CONCERT)
                .venue("테스트 장소")
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();

        return eventRepository.save(event);
    }

    private Seat saveSeat(Event event, String seatNumber, int price) {
        Seat seat = Seat.builder()
                .event(event)
                .seatNumber(seatNumber)
                .price(price)
                .build();

        return seatRepository.save(seat);
    }
}
