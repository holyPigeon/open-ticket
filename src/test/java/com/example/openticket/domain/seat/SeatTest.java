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

    @DisplayName("좌석을 예약하면 좌석 상태가 예약됨으로 변경된다.")
    @Test
    void bookWhenSeatStatusAvailable() {
        // given
        Seat seat = createSeat();

        // when
        seat.book();

        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @DisplayName("예약 가능 상태가 아닌 좌석을 예약하려 할 경우, 예외가 발생한다.")
    @Test
    void throwExceptionWhenSeatStatusNotAvailable() {
        // given
        Seat seat = createSeat();
        seat.book();

        // when, then
        assertThatThrownBy(seat::book)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약할 수 없는 좌석입니다.");
    }

    @DisplayName("예약을 취소하면 좌석 상태가 예약 가능으로 변경된다.")
    @Test
    void cancelBooking() {
        // given
        Seat seat = createSeat();
        seat.book();

        // when
        seat.cancelBooking();

        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @DisplayName("예약 완료 상태가 아닌 좌석에 대해 예약을 취소하려 할 경우, 예외가 발생한다.")
    @Test
    void cancelBookingException1() {
        // given
        Seat seat = createSeat();
        seat.book();

        // when
        seat.cancelBooking();

        // when, then
        assertThatThrownBy(seat::cancelBooking)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예약 취소할 수 없는 좌석입니다.");
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