package com.example.openticket.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import com.example.openticket.domain.seat.Seat;
import com.example.openticket.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookingTest {

    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        user = createUser();
        event = createEvent();
    }
    
    @DisplayName("예약 생성 시, 예약하려는 좌석 리스트의 총 가격을 계산한다.")
    @Test
    void calculateTotalPrice() {
        // given
        List<Seat> seats = List.of(
                createSeat("A1", 10000),
                createSeat("A2", 10000),
                createSeat("A3", 20000)
        );

        // when
        Booking booking = createBooking(LocalDateTime.now(), seats);

        // then
        assertThat(booking.getTotalPrice()).isEqualTo(40000);
    }

    @DisplayName("예약 생성 시, 예약 시간을 기록한다.")
    @Test
    void registeredDateTime() {
        // given
        LocalDateTime bookedAt = LocalDateTime.now();
        List<Seat> seats = createDefaultSeats();

        // when
        Booking booking = createBooking(bookedAt, seats);

        // then
        assertThat(booking.getBookedAt()).isEqualTo(bookedAt);
    }

    @DisplayName("예약 생성 시, 초기 예약 상태는 BOOKED 이다.")
    @Test
    void initialBookingStatus() {
        // given
        List<Seat> seats = createDefaultSeats();

        // when
        Booking booking = createBooking(LocalDateTime.now(), seats);

        // then
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.BOOKED);
    }

    @DisplayName("예약을 취소하면 예약 상태가 취소로 변경된다.")
    @Test
    void cancel() {
        // given
        Booking booking = createDefaultBooking();

        // when
        booking.cancel();

        // then
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @DisplayName("예약 상태가 아닌 예약을 취소하려 할 때, 예외가 발생한다.")
    @Test
    void cancelException1() {
        // given
        Booking booking = createDefaultBooking();
        booking.cancel();

        // when, then
        assertThatThrownBy(booking::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("취소할 수 없는 예약 상태입니다.");
    }

    @DisplayName("")
    @Test
    void isCancelled() {
        // given
        Booking booking = createDefaultBooking();

        // when
        booking.cancel();

        // then
        assertThat(booking.isCancelled()).isTrue();
    }

    private Booking createDefaultBooking() {
        return createBooking(LocalDateTime.now(), createDefaultSeats());
    }

    private Booking createBooking(LocalDateTime bookedAt, List<Seat> seats) {
        return Booking.create(user, bookedAt, seats);
    }

    private List<Seat> createDefaultSeats() {
        Seat seat1 = createSeat("A1", 10000);
        Seat seat2 = createSeat("A2", 10000);
        Seat seat3 = createSeat("A3", 10000);

        return List.of(seat1, seat2, seat3);
    }

    private Seat createSeat(String seatNumber, int price) {
        return Seat.builder()
                .event(event)
                .seatNumber(seatNumber)
                .price(price)
                .build();
    }

    private User createUser() {
        return User.builder()
                .name("test user 1")
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