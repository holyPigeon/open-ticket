package com.example.openticket.domain.booking;

import static com.example.openticket.domain.booking.BookingStatus.BOOKED;
import static org.assertj.core.groups.Tuple.tuple;

import com.example.openticket.IntegrationTestSupport;
import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import com.example.openticket.domain.event.persistence.EventRepository;
import com.example.openticket.domain.seat.Seat;
import com.example.openticket.domain.seat.SeatRepository;
import com.example.openticket.domain.user.User;
import com.example.openticket.domain.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BookingRepositoryTest extends IntegrationTestSupport {

    @Autowired BookingRepository bookingRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;

    @DisplayName("사용자 ID로 예약 내역을 조회할 수 있다.")
    @Test
    void findByUserId() {
        // given
        User user1 = createUser("test user 1");
        User user2 = createUser("test user 2");
        userRepository.saveAll(List.of(user1, user2));

        Event event = createEvent();
        eventRepository.save(event);

        LocalDateTime now = LocalDateTime.now();

        Seat seat1 = createSeat(event, "A1", 10000);
        Seat seat2 = createSeat(event, "A2", 10000);
        Seat seat3 = createSeat(event, "A3", 20000);
        seatRepository.saveAll(List.of(seat1, seat2, seat3));

        Booking booking1 = Booking.create(user1, now, List.of(seat1, seat2));
        Booking booking2 = Booking.create(user2, now, List.of(seat3));
        bookingRepository.saveAll(List.of(booking1, booking2));

        // when
        List<Booking> bookings = bookingRepository.findByUserId(user1.getId());

        // then
        Assertions.assertThat(bookings).hasSize(1)
                .extracting("user.id", "totalPrice", "bookedAt", "status")
                .containsExactlyInAnyOrder(
                        tuple(user1.getId(), 20000, now, BOOKED)
                );
    }

    private User createUser(String name) {
        return User.builder()
                .name(name)
                .build();
    }

    private List<Seat> createSeats(Event event, int... prices) {
        Seat seat1 = createSeat(event, "A1", prices[0]);
        Seat seat2 = createSeat(event, "A2", prices[1]);
        Seat seat3 = createSeat(event, "A3", prices[2]);

        List<Seat> seats = List.of(seat1, seat2, seat3);
        seatRepository.saveAll(seats);

        return seats;
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