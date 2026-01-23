package com.example.openticket.domain.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeatTest {

    private Event event;

    @BeforeEach
    void setUp() {
        event = createEvent();
    }

    @DisplayName("좌석이 예약 가능 상태일 경우, 예약할 수 있다.")
    @Test
    void reserveWhenSeatStatusAvailable() {
        // given
        Seat seat = createSeat();

        // when
        seat.reserve();

        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }

    @DisplayName("좌석이 예약 가능 상태가 아닐 경우, 예외가 발생한다.")
    @Test
    void throwExceptionWhenSeatStatusNotAvailable() {
        // given
        Seat seat = createSeat();

        // when
        seat.reserve();

        // then
        assertThatThrownBy(() -> seat.reserve())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약할 수 없는 좌석입니다.");
    }



    private Seat createSeat() {
        return Seat.builder()
                .event(event)
                .seatNumber("A1")
                .price(10000)
                .build();
    }

    private Event createEvent() {
        return Event.builder()
                .title("test event 1")
                .category(Category.CONCERT)
                .startAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2027, 1, 1, 0, 0))
                .venue("test venue 1")
                .build();
    }
}