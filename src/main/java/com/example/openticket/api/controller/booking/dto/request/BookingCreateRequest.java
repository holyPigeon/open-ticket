package com.example.openticket.api.controller.booking.dto.request;

import com.example.openticket.api.service.booking.dto.request.BookingCreateServiceRequest;
import java.util.List;

public record BookingCreateRequest(
        List<Long> seatIds,
        Long eventId
) {
    public BookingCreateServiceRequest toServiceRequest() {
        return new BookingCreateServiceRequest(this.seatIds);
    }
}
