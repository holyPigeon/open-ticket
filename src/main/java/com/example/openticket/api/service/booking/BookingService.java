package com.example.openticket.api.service.booking;

import com.example.openticket.api.service.booking.dto.request.BookingCreateServiceRequest;
import com.example.openticket.api.service.booking.dto.response.BookingResponse;
import com.example.openticket.domain.booking.Booking;
import com.example.openticket.domain.booking.BookingRepository;
import com.example.openticket.domain.seat.Seat;
import com.example.openticket.domain.seat.SeatRepository;
import com.example.openticket.domain.seat.SeatStatus;
import com.example.openticket.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService implements BookingUseCase {
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public BookingResponse createBooking(User user, BookingCreateServiceRequest request, LocalDateTime now) {
        List<Seat> seats = seatRepository.findAllById(request.seatIds());
        validateSeats(request.seatIds().size(), seats);

        Booking booking = Booking.create(user, now, seats);
        bookingRepository.save(booking);

        return BookingResponse.of(booking);
    }

    private void validateSeats(int seatSize, List<Seat> seats) {
        if (seats.size() != seatSize) {
            throw new IllegalArgumentException("존재하지 않는 좌석이 포함되어 있습니다.");
        }

        boolean hasReservedSeat = seats.stream()
                .anyMatch(seat -> seat.getStatus() != SeatStatus.AVAILABLE);
        if (hasReservedSeat) {
            throw new IllegalArgumentException("이미 예약된 좌석이 포함되어 있습니다.");
        }
    }

    public List<BookingResponse> getUserBookings(User user) {
        List<Booking> bookings = bookingRepository.findByUserId(user.getId());

        return bookings.stream()
                .map(BookingResponse::of)
                .toList();
    }

    public BookingResponse getBookingDetails(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        return BookingResponse.of(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        if (booking.isCancelled()) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        booking.cancel();

        return BookingResponse.of(booking);
    }
}
