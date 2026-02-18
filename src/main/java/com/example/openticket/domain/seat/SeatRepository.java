package com.example.openticket.domain.seat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findAllById(Iterable<Long> ids);

    @Query("SELECT s FROM Seat s JOIN FETCH s.event WHERE s.event.id = :eventId")
    List<Seat> findByEventIdWithEvent(@Param("eventId") Long eventId);
}
