package com.example.openticket.api.service.booking;

import com.example.openticket.api.service.booking.dto.request.BookingCreateServiceRequest;
import com.example.openticket.api.service.booking.dto.response.BookingResponse;
import com.example.openticket.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingUseCase {
    BookingResponse createBooking(User user, BookingCreateServiceRequest request, LocalDateTime now);
    List<BookingResponse> getUserBookings(User user);
    BookingResponse getBookingDetails(Long bookingId);
    BookingResponse cancelBooking(Long bookingId);
}
