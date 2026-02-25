package com.example.openticket.api.controller.booking;

import com.example.openticket.api.ApiResponse;
import com.example.openticket.api.controller.booking.dto.request.BookingCreateRequest;
import com.example.openticket.api.service.booking.BookingService;
import com.example.openticket.api.service.booking.dto.response.BookingResponse;
import com.example.openticket.domain.user.User;
import com.example.openticket.global.auth.LoginUser;
import com.example.openticket.global.annotation.CheckQueueToken;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @CheckQueueToken
    @PostMapping("/api/v1/bookings")
    public ApiResponse<BookingResponse> createBooking(
            @LoginUser User user,
            @Valid @RequestBody BookingCreateRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();

        return ApiResponse.ok(bookingService.createBooking(user, request.toServiceRequest(), now));
    }

    @GetMapping("/api/v1/bookings/my")
    public ApiResponse<List<BookingResponse>> getUserBookings(@LoginUser User user) {
        return ApiResponse.ok(bookingService.getUserBookings(user));
    }

    @GetMapping("/api/v1/bookings/{bookingId}")
    public ApiResponse<BookingResponse> getBookingDetails(@PathVariable Long bookingId) {
        return ApiResponse.ok(bookingService.getBookingDetails(bookingId));
    }

    @PatchMapping("/api/v1/bookings/{bookingId}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(@PathVariable Long bookingId) {
        BookingResponse response = bookingService.cancelBooking(bookingId);

        return ApiResponse.ok(response);
    }
}
