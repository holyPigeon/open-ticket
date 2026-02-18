package com.example.openticket.global.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.openticket.global.queue.EventQueueManager.QueuePhase;
import com.example.openticket.global.queue.EventQueueManager.QueueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventQueueManagerTest {

    private EventQueueManager manager;

    @BeforeEach
    void setUp() {
        manager = new EventQueueManager();
    }

    @DisplayName("첫 번째 사용자가 대기열에 진입하면 즉시 ALLOWED 상태가 된다.")
    @Test
    void enterQueue_firstUser_isAllowed() {
        // given
        Long eventId = 1L;
        Long userId = 100L;

        // when
        QueueEntry entry = manager.enter(eventId, userId);
        QueueStatus status = manager.check(eventId, entry.token());

        // then
        assertThat(status.phase()).isEqualTo(QueuePhase.ALLOWED);
        assertThat(status.remainingSeconds()).isGreaterThan(0);
    }

    @DisplayName("최대 허용 인원을 초과하면 WAITING 상태가 된다.")
    @Test
    void enterQueue_beyondCapacity_isWaiting() {
        // given
        Long eventId = 1L;
        for (long i = 1; i <= 100; i++) {
            manager.enter(eventId, i);
        }

        // when
        QueueEntry entry = manager.enter(eventId, 101L);
        QueueStatus status = manager.check(eventId, entry.token());

        // then
        assertThat(status.phase()).isEqualTo(QueuePhase.WAITING);
        assertThat(status.position()).isEqualTo(1);
    }

    @DisplayName("같은 사용자가 다시 진입하면 기존 토큰을 반환한다.")
    @Test
    void enterQueue_sameUser_returnsExistingToken() {
        // given
        Long eventId = 1L;
        Long userId = 100L;

        // when
        QueueEntry first = manager.enter(eventId, userId);
        QueueEntry second = manager.enter(eventId, userId);

        // then
        assertThat(first.token()).isEqualTo(second.token());
    }

    @DisplayName("활성 토큰을 소비하면 대기 중인 다음 사용자가 승격된다.")
    @Test
    void consumeActiveToken_promotesNextWaiting() {
        // given
        Long eventId = 1L;
        QueueEntry firstEntry = manager.enter(eventId, 1L);
        for (long i = 2; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueEntry waitingEntry = manager.enter(eventId, 101L);

        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.WAITING);

        // when
        boolean consumed = manager.consumeActiveToken(eventId, firstEntry.token());

        // then
        assertThat(consumed).isTrue();
        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("존재하지 않는 토큰으로 상태를 조회하면 예외가 발생한다.")
    @Test
    void checkStatus_unknownToken_throwsException() {
        // when & then
        assertThatThrownBy(() -> manager.check(1L, "unknown-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 대기열 토큰입니다.");
    }

    @DisplayName("존재하지 않는 토큰을 소비하면 false를 반환한다.")
    @Test
    void consumeActiveToken_invalidToken_returnsFalse() {
        // when
        boolean result = manager.consumeActiveToken(1L, "invalid-token");

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("유효한 활성 토큰을 validate하면 true를 반환하고 토큰이 유지된다.")
    @Test
    void validate_activeToken_returnsTrueAndPreservesToken() {
        // given
        Long eventId = 1L;
        QueueEntry entry = manager.enter(eventId, 1L);

        // when
        boolean result = manager.validate(eventId, entry.token());

        // then
        assertThat(result).isTrue();
        assertThat(manager.check(eventId, entry.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("존재하지 않는 토큰을 validate하면 false를 반환한다.")
    @Test
    void validate_invalidToken_returnsFalse() {
        // when
        boolean result = manager.validate(1L, "invalid-token");

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("대기열에서 위치가 정확하게 반환된다.")
    @Test
    void checkStatus_waitingUsers_correctPositions() {
        // given
        Long eventId = 1L;
        for (long i = 1; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueEntry waiting1 = manager.enter(eventId, 101L);
        QueueEntry waiting2 = manager.enter(eventId, 102L);
        QueueEntry waiting3 = manager.enter(eventId, 103L);

        // when & then
        assertThat(manager.check(eventId, waiting1.token()).position()).isEqualTo(1);
        assertThat(manager.check(eventId, waiting2.token()).position()).isEqualTo(2);
        assertThat(manager.check(eventId, waiting3.token()).position()).isEqualTo(3);
    }
}
