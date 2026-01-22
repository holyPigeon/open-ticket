package com.example.openticket.domain.seat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatStatus {

    AVAILABLE("예약 가능"),
    RESERVED("예약중"),
    SOLD("판매 완료");

    private final String name;

}
