package com.example.openticket.domain.booking;

import static com.example.openticket.domain.booking.BookingStatus.BOOKED;
import static org.assertj.core.groups.Tuple.tuple;

import com.example.openticket.support.IntegrationTestSupport;
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

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @DisplayName("사용자 ID로 예약 내역을 조회할 수 있다.")
    @Test
    void findByUserId() {
        // given
        User user1 = saveUser("test user 1", "test email 1", "test password 1");
        User user2 = saveUser("test user 2", "test email 2", "test password 2");
        Event event = saveSampleEvent();

        LocalDateTime now = LocalDateTime.now();

        Seat seat1 = saveSeat(event, "A1", 10000);
        Seat seat2 = saveSeat(event, "A2", 10000);
        Seat seat3 = saveSeat(event, "A3", 20000);

        saveBooking(user1, now, List.of(seat1, seat2));
        saveBooking(user2, now, List.of(seat3));

        // when
        List<Booking> bookings = bookingRepository.findByUserId(user1.getId());

        // then
        Assertions.assertThat(bookings).hasSize(1)
                .extracting("user.id", "totalPrice", "bookedAt", "status")
                .containsExactlyInAnyOrder(
                        tuple(user1.getId(), 20000, now, BOOKED)
                );
    }

    private void saveBooking(User user, LocalDateTime bookedAt, List<Seat> seats) {
        Booking booking = Booking.create(user, bookedAt, seats);
        bookingRepository.save(booking);
    }

    private User saveUser(String name, String email, String password) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        return userRepository.save(user);
    }

    private Seat saveSeat(Event event, String seatNumber, int price) {
        Seat seat = Seat.builder()
                .event(event)
                .seatNumber(seatNumber)
                .price(price)
                .build();

        return seatRepository.save(seat);
    }

    private Event saveSampleEvent() {
        Event event = Event.builder()
                .title("test event 1")
                .category(Category.CONCERT)
                .venue("test venue 1")
                .startAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endAt(LocalDateTime.of(2027, 1, 1, 0, 0))
                .build();

        return eventRepository.save(event);
    }
}