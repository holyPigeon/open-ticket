package com.example.openticket.infrastructure.booking;

import com.example.openticket.api.service.booking.BookingService;
import com.example.openticket.api.service.booking.BookingUseCase;
import com.example.openticket.api.service.booking.dto.request.BookingCreateServiceRequest;
import com.example.openticket.api.service.booking.dto.response.BookingResponse;
import com.example.openticket.domain.user.User;
import com.example.openticket.global.exception.LockAcquisitionException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "queue.type", havingValue = "redis")
public class BookingLockFacade implements BookingUseCase {

    private static final String SEAT_LOCK_PREFIX = "booking:lock:seat:";

    private final BookingService bookingService;
    private final RedissonClient redissonClient;

    @Override
    public BookingResponse createBooking(User user, BookingCreateServiceRequest request, LocalDateTime now) {
        RLock[] locks = request.seatIds().stream()
                .sorted()
                .map(id -> redissonClient.getLock(SEAT_LOCK_PREFIX + id))
                .toArray(RLock[]::new);
        RLock multiLock = redissonClient.getMultiLock(locks);
        try {
            if (!multiLock.tryLock(0, TimeUnit.SECONDS)) {
                throw new LockAcquisitionException("좌석 락 획득에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }
            return bookingService.createBooking(user, request, now);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("좌석 락 획득 중 인터럽트가 발생했습니다.");
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }

    @Override
    public List<BookingResponse> getUserBookings(User user) {
        return bookingService.getUserBookings(user);
    }

    @Override
    public BookingResponse getBookingDetails(Long bookingId) {
        return bookingService.getBookingDetails(bookingId);
    }

    @Override
    public BookingResponse cancelBooking(Long bookingId) {
        return bookingService.cancelBooking(bookingId);
    }
}
