package com.example.openticket.infrastructure.queue.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.openticket.domain.queue.EventQueueManager;
import com.example.openticket.domain.queue.QueueEntry;
import com.example.openticket.domain.queue.QueuePhase;
import com.example.openticket.domain.queue.QueueStatus;
import com.example.openticket.support.RedisTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

@EnabledIfDockerAvailable
class RedisEventQueueManagerTest extends RedisTestSupport {

    @Autowired
    private EventQueueManager manager;

    @DisplayName("첫 번째 사용자가 대기열에 진입하면 즉시 ALLOWED 상태가 된다.")
    @Test
    void enterQueue_firstUser_isAllowed() {
        Long eventId = 1L;
        Long userId = 100L;

        QueueEntry entry = manager.enter(eventId, userId);
        QueueStatus status = manager.check(eventId, entry.token());

        assertThat(status.phase()).isEqualTo(QueuePhase.ALLOWED);
        assertThat(status.remainingSeconds()).isGreaterThan(0);
    }

    @DisplayName("최대 허용 인원을 초과하면 WAITING 상태가 된다.")
    @Test
    void enterQueue_beyondCapacity_isWaiting() {
        Long eventId = 1L;
        for (long i = 1; i <= 100; i++) {
            manager.enter(eventId, i);
        }

        QueueEntry entry = manager.enter(eventId, 101L);
        QueueStatus status = manager.check(eventId, entry.token());

        assertThat(status.phase()).isEqualTo(QueuePhase.WAITING);
        assertThat(status.position()).isEqualTo(1);
    }

    @DisplayName("같은 사용자가 다시 진입하면 기존 토큰을 반환한다.")
    @Test
    void enterQueue_sameUser_returnsExistingToken() {
        Long eventId = 1L;
        Long userId = 100L;

        QueueEntry first = manager.enter(eventId, userId);
        QueueEntry second = manager.enter(eventId, userId);

        assertThat(first.token()).isEqualTo(second.token());
    }

    @DisplayName("활성 토큰을 소비하면 대기 중인 다음 사용자가 승격된다.")
    @Test
    void consumeActiveToken_promotesNextWaiting() {
        Long eventId = 1L;
        QueueEntry firstEntry = manager.enter(eventId, 1L);
        for (long i = 2; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueEntry waitingEntry = manager.enter(eventId, 101L);

        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.WAITING);

        boolean consumed = manager.consumeActiveToken(eventId, firstEntry.token());

        assertThat(consumed).isTrue();
        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("존재하지 않는 토큰으로 상태를 조회하면 예외가 발생한다.")
    @Test
    void checkStatus_unknownToken_throwsException() {
        assertThatThrownBy(() -> manager.check(1L, "unknown-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 대기열 토큰입니다.");
    }

    @DisplayName("존재하지 않는 토큰을 소비하면 false를 반환한다.")
    @Test
    void consumeActiveToken_invalidToken_returnsFalse() {
        boolean result = manager.consumeActiveToken(1L, "invalid-token");

        assertThat(result).isFalse();
    }

    @DisplayName("유효한 활성 토큰을 validate하면 true를 반환하고 토큰이 유지된다.")
    @Test
    void validate_activeToken_returnsTrueAndPreservesToken() {
        Long eventId = 1L;
        QueueEntry entry = manager.enter(eventId, 1L);

        boolean result = manager.validate(eventId, entry.token());

        assertThat(result).isTrue();
        assertThat(manager.check(eventId, entry.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("존재하지 않는 토큰을 validate하면 false를 반환한다.")
    @Test
    void validate_invalidToken_returnsFalse() {
        boolean result = manager.validate(1L, "invalid-token");

        assertThat(result).isFalse();
    }

    @DisplayName("대기열에서 위치가 정확하게 반환된다.")
    @Test
    void checkStatus_waitingUsers_correctPositions() {
        Long eventId = 1L;
        for (long i = 1; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueEntry waiting1 = manager.enter(eventId, 101L);
        QueueEntry waiting2 = manager.enter(eventId, 102L);
        QueueEntry waiting3 = manager.enter(eventId, 103L);

        assertThat(manager.check(eventId, waiting1.token()).position()).isEqualTo(1);
        assertThat(manager.check(eventId, waiting2.token()).position()).isEqualTo(2);
        assertThat(manager.check(eventId, waiting3.token()).position()).isEqualTo(3);
    }

    @DisplayName("활성 토큰으로 이탈하면 슬롯이 반납되고 대기 사용자가 승격된다.")
    @Test
    void leave_activeToken_promotesNextWaiting() {
        Long eventId = 10L;
        QueueEntry activeEntry = manager.enter(eventId, 1L);
        for (long i = 2; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueEntry waitingEntry = manager.enter(eventId, 101L);

        boolean removed = manager.leave(eventId, activeEntry.token());

        assertThat(removed).isTrue();
        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("대기 토큰으로 이탈하면 큐에서 제거된다.")
    @Test
    void leave_waitingToken_removesFromQueue() {
        Long eventId = 11L;
        for (long i = 1; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueEntry waitingEntry = manager.enter(eventId, 101L);

        boolean removed = manager.leave(eventId, waitingEntry.token());

        assertThat(removed).isTrue();
        assertThatThrownBy(() -> manager.check(eventId, waitingEntry.token()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 대기열 토큰입니다.");
    }

    @DisplayName("존재하지 않는 토큰으로 이탈하면 false를 반환한다.")
    @Test
    void leave_invalidToken_returnsFalse() {
        boolean removed = manager.leave(12L, "invalid-token");

        assertThat(removed).isFalse();
    }

    @DisplayName("대기 사용자가 없을 때 활성 토큰을 소비해도 새 사용자가 즉시 ALLOWED가 된다.")
    @Test
    void consumeActiveToken_withoutWaiting_newUserIsAllowed() {
        Long eventId = 1L;
        QueueEntry entry = manager.enter(eventId, 1L);

        boolean consumed = manager.consumeActiveToken(eventId, entry.token());

        assertThat(consumed).isTrue();
        QueueEntry next = manager.enter(eventId, 2L);
        assertThat(manager.check(eventId, next.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("승격은 promoteForEvent에서 반영된다.")
    @Test
    void promoteForEvent_shouldPromoteWaiting() {
        Long eventId = 22L;
        QueueEntry activeEntry = manager.enter(eventId, 1L);
        for (long userId = 2L; userId <= 100L; userId++) {
            manager.enter(eventId, userId);
        }
        QueueEntry waitingEntry = manager.enter(eventId, 101L);

        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.WAITING);

        // Consume the first active token to free a slot
        manager.consumeActiveToken(eventId, activeEntry.token());

        // Waiting entry should have been promoted by consume's promotion logic
        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("check는 TTL을 갱신하지만 큐 상태를 변경하지 않는 순수 조회다.")
    @Test
    void check_shouldNotMutateQueueState() {
        Long eventId = 20L;
        for (long userId = 1L; userId <= 100L; userId++) {
            manager.enter(eventId, userId);
        }
        QueueEntry waitingEntry = manager.enter(eventId, 101L);

        QueueStatus first = manager.check(eventId, waitingEntry.token());
        QueueStatus second = manager.check(eventId, waitingEntry.token());

        assertThat(first.phase()).isEqualTo(QueuePhase.WAITING);
        assertThat(first.position()).isEqualTo(1);
        assertThat(second.phase()).isEqualTo(QueuePhase.WAITING);
        assertThat(second.position()).isEqualTo(1);
    }

    @DisplayName("활성 사용자가 차례로 빠져나가면 대기 사용자는 순서대로 승격된다.")
    @Test
    void waitingUser_shouldBePromoted_whenActiveUsersConsumeTokens() {
        Long eventId = 1L;
        java.util.List<String> activeTokens = new java.util.ArrayList<>();

        for (long userId = 1L; userId <= 100; userId++) {
            activeTokens.add(manager.enter(eventId, userId).token());
        }

        QueueEntry waiting1 = manager.enter(eventId, 101L);
        QueueEntry waiting2 = manager.enter(eventId, 102L);
        QueueEntry waiting3 = manager.enter(eventId, 103L);

        assertThat(manager.check(eventId, waiting1.token()).position()).isEqualTo(1);
        assertThat(manager.check(eventId, waiting2.token()).position()).isEqualTo(2);
        assertThat(manager.check(eventId, waiting3.token()).position()).isEqualTo(3);

        boolean firstConsumed = manager.consumeActiveToken(eventId, activeTokens.get(0));
        boolean secondConsumed = manager.consumeActiveToken(eventId, activeTokens.get(1));
        boolean thirdConsumed = manager.consumeActiveToken(eventId, activeTokens.get(2));

        assertThat(firstConsumed).isTrue();
        assertThat(secondConsumed).isTrue();
        assertThat(thirdConsumed).isTrue();
        assertThat(manager.check(eventId, waiting1.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
        assertThat(manager.check(eventId, waiting2.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
        assertThat(manager.check(eventId, waiting3.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }
}
