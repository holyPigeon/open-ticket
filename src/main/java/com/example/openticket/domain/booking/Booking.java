package com.example.openticket.domain.booking;

import com.example.openticket.domain.BaseEntity;
import com.example.openticket.domain.bookingSeat.BookingSeat;
import com.example.openticket.domain.seat.Seat;
import com.example.openticket.domain.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "bookings")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    @Column(nullable = false)
    private int totalPrice;

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    private BookingStatus status;

    private Booking(User user, LocalDateTime bookedAt, List<Seat> seats) {
        this.user = user;
        this.totalPrice = calculateTotalPrice(seats);
        this.bookedAt = bookedAt;
        this.status = BookingStatus.BOOKED;
        this.bookingSeats = seats.stream()
                .map(seat -> {
                    seat.reserve();
                    return new BookingSeat(this, seat);
                }).collect(Collectors.toList());
    }

    public static Booking create(User user, LocalDateTime bookedAt, List<Seat> seats) {
        return new Booking(user, bookedAt, seats);
    }

    public void cancel() {
        if (this.status != BookingStatus.BOOKED) {
            throw new IllegalStateException("취소할 수 없는 예약 상태입니다.");
        }
        this.status = BookingStatus.CANCELLED;
        bookingSeats.forEach(bookingSeat -> bookingSeat.getSeat().cancelReservation());
    }

    public boolean isCancelled() {
        return this.status == BookingStatus.CANCELLED;
    }

    private int calculateTotalPrice(List<Seat> seats) {
        return seats.stream()
                .mapToInt(Seat::getPrice)
                .sum();
    }
}