package com.example.openticket.api.service.seat;

import com.example.openticket.api.service.seat.dto.response.SeatResponse;
import com.example.openticket.domain.seat.SeatRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;

    public List<SeatResponse> getSeats(Long eventId) {
        return seatRepository.findByEventIdWithEvent(eventId).stream()
                .map(SeatResponse::of)
                .toList();
    }
}
