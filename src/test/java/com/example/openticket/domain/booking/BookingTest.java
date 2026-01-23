package com.example.openticket.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;

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
        int seatPrice1 = 10000;
        int seatPrice2 = 10000;
        int seatPrice3 = 20000;
        List<Seat> seats = createSeats(seatPrice1, seatPrice2, seatPrice3);

        // when
        Booking booking = createBooking(LocalDateTime.now(), seats);

        // then
        int seatPriceSum = seatPrice1 + seatPrice2 + seatPrice3;
        assertThat(booking.getTotalPrice()).isEqualTo(seatPriceSum);
    }

    @DisplayName("예약 생성 시, 예약 시간을 기록한다.")
    @Test
    void registeredDateTime() {
        // given
        LocalDateTime bookedAt = LocalDateTime.now();
        List<Seat> seats = createSeats();

        // when
        Booking booking = createBooking(bookedAt, seats);

        // then
        assertThat(booking.getBookedAt()).isEqualTo(bookedAt);
    }

    @DisplayName("예약 생성 시, 초기 예약 상태는 BOOKED 이다.")
    @Test
    void initialBookingStatus() {
        // given
        List<Seat> seats = createSeats();

        // when
        Booking booking = createBooking(LocalDateTime.now(), seats);

        // then
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.BOOKED);
    }

    private Booking createBooking(LocalDateTime bookedAt, List<Seat> seats) {
        return Booking.create(user, bookedAt, seats);
    }

    private User createUser() {
        return User.builder()
                .name("test user 1")
                .build();
    }

    private List<Seat> createSeats(int... prices) {
        Seat seat1 = createSeat(event, "001", prices[0]);
        Seat seat2 = createSeat(event, "002", prices[1]);
        Seat seat3 = createSeat(event, "003", prices[2]);

        return List.of(seat1, seat2, seat3);
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
                .startAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2027, 1, 1, 0, 0))
                .venue("test venue 1")
                .build();
    }
}