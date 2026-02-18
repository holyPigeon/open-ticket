package com.example.openticket.api.controller.booking.dto.request;

import com.example.openticket.api.service.booking.dto.request.BookingCreateServiceRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BookingCreateRequest(
        @NotEmpty(message = "좌석 ID는 필수입니다.") List<Long> seatIds
) {
    public BookingCreateServiceRequest toServiceRequest() {
        return new BookingCreateServiceRequest(this.seatIds);
    }
}
