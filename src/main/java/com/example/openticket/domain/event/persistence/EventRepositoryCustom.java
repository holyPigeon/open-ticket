package com.example.openticket.domain.event.persistence;

import com.example.openticket.domain.event.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRepositoryCustom {

    Page<Event> findAllWithSearchCondition(EventSearchCondition searchCondition, Pageable pageable);
}
