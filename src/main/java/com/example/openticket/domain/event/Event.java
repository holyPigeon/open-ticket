package com.example.openticket.domain.event;

import com.example.openticket.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private String venue;

    @Builder
    private Event(String title, Category category, LocalDateTime startAt, LocalDateTime endAt, String venue) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("시작 시간 및 종료 시간은 null일 수 없습니다.");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 이전이어야 합니다.");
        }

        this.title = title;
        this.category = category;
        this.startAt = startAt;
        this.endAt = endAt;
        this.venue = venue;
    }
}
