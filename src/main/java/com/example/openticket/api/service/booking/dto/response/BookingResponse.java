package com.example.openticket.api.service.booking.dto.response;

import com.example.openticket.api.service.seat.dto.response.SeatResponse;
import com.example.openticket.api.service.user.dto.response.UserResponse;
import com.example.openticket.domain.booking.Booking;
import com.example.openticket.domain.booking.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        UserResponse user,
        List<SeatResponse> seats,
        LocalDateTime bookedAt,
        int totalPrice,
        BookingStatus status
) {

    public static BookingResponse of(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                UserResponse.of(booking.getUser()),
                booking.getBookingSeats().stream()
                        .map(seat -> SeatResponse.of(seat.getSeat()))
                        .toList(),
                booking.getBookedAt(),
                booking.getTotalPrice(),
                booking.getStatus()
        );
    }
}
