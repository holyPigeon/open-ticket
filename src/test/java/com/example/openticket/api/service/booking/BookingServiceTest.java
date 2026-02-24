package com.example.openticket.api.service.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import com.example.openticket.support.IntegrationTestSupport;
import com.example.openticket.api.service.booking.dto.request.BookingCreateServiceRequest;
import com.example.openticket.api.service.booking.dto.response.BookingResponse;
import com.example.openticket.domain.booking.Booking;
import com.example.openticket.domain.booking.BookingRepository;
import com.example.openticket.domain.booking.BookingStatus;
import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import com.example.openticket.domain.event.persistence.EventRepository;
import com.example.openticket.domain.seat.Seat;
import com.example.openticket.domain.seat.SeatRepository;
import com.example.openticket.domain.seat.SeatStatus;
import com.example.openticket.domain.user.User;
import com.example.openticket.domain.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BookingServiceTest extends IntegrationTestSupport {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @DisplayName("좌석을 선택하여 예약을 생성하면, 예약 정보가 저장되고 좌석은 예약 불가능 상태가 된다.")
    @Test
    void createBooking() {
        // given
        User user = saveSampleUser();
        Event event = saveSampleEvent();
        Seat seat1 = saveSeat(event, "A1");
        Seat seat2 = saveSeat(event, "A2");

        BookingCreateServiceRequest request = new BookingCreateServiceRequest(
                List.of(seat1.getId(), seat2.getId())
        );

        LocalDateTime now = LocalDateTime.now();

        // when
        BookingResponse response = bookingService.createBooking(user, request, now);

        // then
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(BookingStatus.BOOKED);
        assertThat(response.bookedAt()).isEqualTo(now);

        Booking savedBooking = bookingRepository.findById(response.id()).orElseThrow();
        assertThat(savedBooking.getBookingSeats()).hasSize(2)
                .extracting("seat.seatNumber", "seat.status")
                .containsExactlyInAnyOrder(
                        tuple("A1", SeatStatus.BOOKED),
                        tuple("A2", SeatStatus.BOOKED)
                );
    }

    @DisplayName("예약 생성 시 존재하지 않는 좌석 ID가 포함되어 있으면 예외가 발생한다.")
    @Test
    void createBookingWithInvalidSeatId() {
        // given
        User user = saveSampleUser();
        Event event = saveSampleEvent();
        Seat seat1 = saveSeat(event, "A1");

        BookingCreateServiceRequest request = new BookingCreateServiceRequest(
                List.of(seat1.getId(), 9999L)
        );
        LocalDateTime now = LocalDateTime.now();

        // when, then
        assertThatThrownBy(() -> bookingService.createBooking(user, request, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 좌석이 포함되어 있습니다.");
    }

    @DisplayName("이미 다른 사람이 예약한 좌석을 선택하면 예외가 발생한다.")
    @Test
    void createBookingWithReservedSeat() {
        // given
        User user = saveSampleUser();
        Event event = saveSampleEvent();
        Seat seat1 = saveSeat(event, "A1");
        Seat seat2 = saveSeat(event, "A2");
        seat2.book();

        BookingCreateServiceRequest request = new BookingCreateServiceRequest(
                List.of(seat1.getId(), seat2.getId())
        );

        LocalDateTime now = LocalDateTime.now();

        // when, then
        assertThatThrownBy(() -> bookingService.createBooking(user, request, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 예약된 좌석이 포함되어 있습니다.");
    }

    @DisplayName("사용자의 예약 목록을 조회한다.")
    @Test
    void getUserBookings() {
        // given
        User user = saveSampleUser();
        Event event = saveSampleEvent();
        Seat seat = saveSeat(event, "A1");
        saveBooking(user, LocalDateTime.now(), List.of(seat));

        // when
        List<BookingResponse> result = bookingService.getUserBookings(user);

        // then
        assertThat(result).hasSize(1)
                .extracting("user.name")
                .contains("user 1");
    }

    @DisplayName("존재하지 않는 예약 ID를 조회하면 예외가 발생한다.")
    @Test
    void getBookingDetailsFail() {
        // when & then
        assertThatThrownBy(() -> bookingService.getBookingDetails(9999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking not found: 9999");
    }

    @DisplayName("예약을 취소하면 예약 상태가 취소됨으로 변경되고, 좌석은 다시 예매 가능 상태가 된다.")
    @Test
    void cancelBooking() {
        // given
        User user = saveSampleUser();
        Event event = saveSampleEvent();
        Seat seat1 = saveSeat(event, "A1");
        Seat seat2 = saveSeat(event, "A2");

        Booking booking = saveBooking(user, LocalDateTime.now(), List.of(seat1, seat2));

        // when
        BookingResponse response = bookingService.cancelBooking(booking.getId());

        // then
        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);

        List<Seat> seats = seatRepository.findAllById(List.of(seat1.getId(), seat2.getId()));
        assertThat(seats).extracting("status")
                .containsOnly(SeatStatus.AVAILABLE);
    }

    @DisplayName("이미 취소된 예약을 다시 취소하려고 하면 예외가 발생한다.")
    @Test
    void cancelBookingAlreadyCancelled() {
        // given
        User user = saveSampleUser();
        Event event = saveSampleEvent();
        Seat seat = saveSeat(event, "A1");

        Booking booking = saveBooking(user, LocalDateTime.now(), List.of(seat));
        booking.cancel();

        // when & then
        Long bookingId = booking.getId();
        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 취소된 예약입니다.");
    }

    private Booking saveBooking(User user, LocalDateTime bookedAt, List<Seat> seats) {
        Booking booking = Booking.create(user, bookedAt, seats);

        return bookingRepository.save(booking);
    }

    private User saveSampleUser() {
        User user = User.builder()
                .name("user 1")
                .email("email 1")
                .password("password 1")
                .build();

        return userRepository.save(user);
    }

    private Event saveSampleEvent() {
        Event event = Event.builder()
                .title("event 1")
                .category(Category.CONCERT)
                .venue("venue 1")
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();

        return eventRepository.save(event);
    }

    private Seat saveSeat(Event event, String seatNumber) {
        Seat seat = Seat.builder()
                .event(event)
                .seatNumber(seatNumber)
                .price(10000)
                .build();

        return seatRepository.save(seat);
    }
}