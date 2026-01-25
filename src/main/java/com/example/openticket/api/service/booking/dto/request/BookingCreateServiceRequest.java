package com.example.openticket.api.service.booking.dto.request;

import java.util.List;

public record BookingCreateServiceRequest(
        List<Long> seatIds,
        Long eventId
) {
}
