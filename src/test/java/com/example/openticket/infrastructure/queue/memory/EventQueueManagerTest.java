package com.example.openticket.infrastructure.queue.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.openticket.domain.queue.ActiveToken;
import com.example.openticket.domain.queue.QueuePhase;
import com.example.openticket.domain.queue.QueueStatus;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventQueueManagerTest {

    private InMemoryEventQueueManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryEventQueueManager();
    }

    @DisplayName("첫 번째 사용자가 대기열에 진입하면 즉시 ALLOWED 상태가 된다.")
    @Test
    void enterQueue_firstUser_isAllowed() {
        // given
        Long eventId = 1L;
        Long userId = 100L;

        // when
        QueueStatus entry = manager.enter(eventId, userId);
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
        QueueStatus entry = manager.enter(eventId, 101L);
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
        QueueStatus first = manager.enter(eventId, userId);
        QueueStatus second = manager.enter(eventId, userId);

        // then
        assertThat(first.token()).isEqualTo(second.token());
    }

    @DisplayName("활성 토큰을 소비하면 대기 중인 다음 사용자가 승격된다.")
    @Test
    void consumeActiveToken_promotesNextWaiting() {
        // given
        Long eventId = 1L;
        QueueStatus firstEntry = manager.enter(eventId, 1L);
        for (long i = 2; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueStatus waitingEntry = manager.enter(eventId, 101L);

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
        QueueStatus entry = manager.enter(eventId, 1L);

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
        QueueStatus waiting1 = manager.enter(eventId, 101L);
        QueueStatus waiting2 = manager.enter(eventId, 102L);
        QueueStatus waiting3 = manager.enter(eventId, 103L);

        // when & then
        assertThat(manager.check(eventId, waiting1.token()).position()).isEqualTo(1);
        assertThat(manager.check(eventId, waiting2.token()).position()).isEqualTo(2);
        assertThat(manager.check(eventId, waiting3.token()).position()).isEqualTo(3);
    }

    @DisplayName("대기 사용자가 없을 때 활성 토큰을 소비해도 슬롯 카운트 누수가 없다.")
    @Test
    void consumeActiveToken_withoutWaiting_shouldNotLeakActiveSlot() {
        // given
        Long eventId = 1L;
        QueueStatus entry = manager.enter(eventId, 1L);
        assertThat(manager.activeCount(eventId)).isEqualTo(1);

        // when
        boolean consumed = manager.consumeActiveToken(eventId, entry.token());

        // then
        assertThat(consumed).isTrue();
        assertThat(manager.activeCount(eventId)).isEqualTo(0);
        QueueStatus next = manager.enter(eventId, 2L);
        assertThat(manager.check(eventId, next.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("존재하지 않는 토큰 소비를 반복해도 슬롯 카운트는 음수가 되지 않는다.")
    @Test
    void consumeInvalidToken_repeated_shouldNotMakeActiveCountNegative() {
        // given
        Long eventId = 1L;

        // when
        for (int i = 0; i < 1000; i++) {
            manager.consumeActiveToken(eventId, "invalid-token-" + i);
        }

        // then
        assertThat(manager.activeCount(eventId)).isGreaterThanOrEqualTo(0);
    }

    @DisplayName("만료된 활성 토큰을 consume하면 false를 반환하고 상태를 변경하지 않는다.")
    @Test
    void consumeExpiredToken_shouldReturnFalseWithoutMutatingState() throws Exception {
        // given
        Long eventId = 2L;
        QueueStatus activeEntry = manager.enter(eventId, 1L);
        for (long userId = 2L; userId <= 100L; userId++) {
            manager.enter(eventId, userId);
        }
        QueueStatus waitingEntry = manager.enter(eventId, 101L);
        expireActiveToken(eventId, activeEntry.token(), 1L);
        assertThat(manager.activeCount(eventId)).isEqualTo(100);
        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.WAITING);

        // when
        boolean consumed = manager.consumeActiveToken(eventId, activeEntry.token());

        // then
        assertThat(consumed).isFalse();
        assertThat(manager.activeCount(eventId)).isEqualTo(100);
        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.WAITING);
    }

    @DisplayName("활성 토큰으로 이탈하면 슬롯이 반납되고 대기 사용자가 승격된다.")
    @Test
    void leave_activeToken_promotesNextWaiting() {
        // given
        Long eventId = 10L;
        QueueStatus activeEntry = manager.enter(eventId, 1L);
        for (long i = 2; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueStatus waitingEntry = manager.enter(eventId, 101L);

        // when
        boolean removed = manager.leave(eventId, activeEntry.token());

        // then
        assertThat(removed).isTrue();
        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("대기 토큰으로 이탈하면 큐에서 제거된다.")
    @Test
    void leave_waitingToken_removesFromQueue() {
        // given
        Long eventId = 11L;
        for (long i = 1; i <= 100; i++) {
            manager.enter(eventId, i);
        }
        QueueStatus waitingEntry = manager.enter(eventId, 101L);

        // when
        boolean removed = manager.leave(eventId, waitingEntry.token());

        // then
        assertThat(removed).isTrue();
        assertThatThrownBy(() -> manager.check(eventId, waitingEntry.token()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 대기열 토큰입니다.");
    }

    @DisplayName("존재하지 않는 토큰으로 이탈하면 false를 반환한다.")
    @Test
    void leave_invalidToken_returnsFalse() {
        // when
        boolean removed = manager.leave(12L, "invalid-token");

        // then
        assertThat(removed).isFalse();
    }

    @DisplayName("check는 만료/승격을 트리거하지 않는 순수 조회다.")
    @Test
    void check_shouldNotMutateState() throws Exception {
        // given
        Long eventId = 20L;
        QueueStatus activeEntry = manager.enter(eventId, 1L);
        for (long userId = 2L; userId <= 100L; userId++) {
            manager.enter(eventId, userId);
        }
        QueueStatus waitingEntry = manager.enter(eventId, 101L);
        expireActiveToken(eventId, activeEntry.token(), 1L);
        assertThat(manager.activeCount(eventId)).isEqualTo(100);

        // when
        QueueStatus first = manager.check(eventId, waitingEntry.token());
        QueueStatus second = manager.check(eventId, waitingEntry.token());

        // then
        assertThat(first.phase()).isEqualTo(QueuePhase.WAITING);
        assertThat(first.position()).isEqualTo(1);
        assertThat(second.phase()).isEqualTo(QueuePhase.WAITING);
        assertThat(second.position()).isEqualTo(1);
        assertThat(manager.activeCount(eventId)).isEqualTo(100);
    }

    @DisplayName("만료된 활성 토큰은 check에서 유효하지 않은 토큰으로 처리된다.")
    @Test
    void check_expiredActiveToken_throwsException() throws Exception {
        // given
        Long eventId = 21L;
        QueueStatus activeEntry = manager.enter(eventId, 1L);
        expireActiveToken(eventId, activeEntry.token(), 1L);

        // when & then
        assertThatThrownBy(() -> manager.check(eventId, activeEntry.token()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 대기열 토큰입니다.");
        assertThat(manager.activeCount(eventId)).isEqualTo(1);
    }

    @DisplayName("승격은 check가 아니라 스케줄러 경로에서 반영된다.")
    @Test
    void evictExpired_shouldPromoteWaiting() throws Exception {
        // given
        Long eventId = 22L;
        QueueStatus activeEntry = manager.enter(eventId, 1L);
        for (long userId = 2L; userId <= 100L; userId++) {
            manager.enter(eventId, userId);
        }
        QueueStatus waitingEntry = manager.enter(eventId, 101L);
        expireActiveToken(eventId, activeEntry.token(), 1L);

        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.WAITING);

        // when
        manager.promoteForEvent(eventId);

        // then
        assertThat(manager.check(eventId, waitingEntry.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @SuppressWarnings("unchecked")
    private void expireActiveToken(Long eventId, String token, Long userId) throws Exception {
        Field activeTokensField = InMemoryEventQueueManager.class.getDeclaredField("activeTokens");
        activeTokensField.setAccessible(true);

        ConcurrentHashMap<Long, ConcurrentHashMap<String, ActiveToken>> activeTokens =
                (ConcurrentHashMap<Long, ConcurrentHashMap<String, ActiveToken>>) activeTokensField.get(manager);
        activeTokens.computeIfAbsent(eventId, key -> new ConcurrentHashMap<>())
                .put(token, new ActiveToken(userId, System.currentTimeMillis() - 1_000));
    }
}
