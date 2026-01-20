package com.example.openticket.domain.booking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingStatus {

    BOOKED("예약 완료"),
    CANCELLED("예약 취소");

    private final String name;
}
