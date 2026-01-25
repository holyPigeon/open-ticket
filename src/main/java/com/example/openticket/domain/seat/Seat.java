package com.example.openticket.domain.seat;

import com.example.openticket.domain.BaseEntity;
import com.example.openticket.domain.event.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "seats")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false)
    private SeatStatus status;

    @Builder
    private Seat(Event event, String seatNumber, int price) {
        this.event = event;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    public void reserve() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("예약할 수 없는 좌석입니다.");
        }
        this.status = SeatStatus.RESERVED;
    }

    public void cancelReservation() {
        if (this.status != SeatStatus.RESERVED) {
            throw new IllegalStateException("예약 취소할 수 없는 좌석입니다.");
        }
        this.status = SeatStatus.AVAILABLE;
    }
}