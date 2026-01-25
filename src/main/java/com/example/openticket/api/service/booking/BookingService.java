package com.example.openticket.api.service.booking;

import com.example.openticket.api.service.booking.dto.request.BookingCreateServiceRequest;
import com.example.openticket.api.service.booking.dto.response.BookingResponse;
import com.example.openticket.domain.booking.Booking;
import com.example.openticket.domain.booking.BookingRepository;
import com.example.openticket.domain.seat.Seat;
import com.example.openticket.domain.seat.SeatRepository;
import com.example.openticket.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;

    public BookingResponse createBooking(User user, BookingCreateServiceRequest request, LocalDateTime now) {
        List<Seat> seats = request.seatIds().stream()
                .map(seatId -> seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId))
                ).toList();
        Booking booking = Booking.create(user, now, seats);
        bookingRepository.save(booking);

        return BookingResponse.of(booking);
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

    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        booking.cancel();

        return BookingResponse.of(booking);
    }
}
