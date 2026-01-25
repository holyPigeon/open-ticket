package com.example.openticket.api.service.seat.dto.response;

import com.example.openticket.api.service.event.dto.response.EventResponse;
import com.example.openticket.domain.seat.Seat;
import com.example.openticket.domain.seat.SeatStatus;

public record SeatResponse(
        Long id,
        EventResponse event,
        String seatNumber,
        int price,
        SeatStatus status
) {
    public static SeatResponse of(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                EventResponse.from(seat.getEvent()),
                seat.getSeatNumber(),
                seat.getPrice(),
                seat.getStatus(
        )
        );
    }
}
