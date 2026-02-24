package com.example.openticket.api.service.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.openticket.api.service.queue.dto.response.QueueStatusResponse;
import com.example.openticket.global.queue.QueuePhase;
import com.example.openticket.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QueueServiceTest extends IntegrationTestSupport {

    @Autowired
    private QueueService queueService;

    @DisplayName("대기열에 진입하면 토큰과 ALLOWED 상태를 반환한다.")
    @Test
    void enterQueue() {
        // when
        QueueStatusResponse response = queueService.enterQueue(1L, 100L);

        // then
        assertThat(response.token()).isNotBlank();
        assertThat(response.phase()).isEqualTo(QueuePhase.ALLOWED);
        assertThat(response.remainingSeconds()).isGreaterThan(0);
    }

    @DisplayName("같은 사용자가 같은 이벤트에 재진입하면 기존 토큰을 반환한다.")
    @Test
    void enterQueueDuplicate() {
        // given
        QueueStatusResponse first = queueService.enterQueue(2L, 200L);

        // when
        QueueStatusResponse second = queueService.enterQueue(2L, 200L);

        // then
        assertThat(second.token()).isEqualTo(first.token());
    }

    @DisplayName("유효한 토큰으로 대기열 상태를 조회하면 현재 상태를 반환한다.")
    @Test
    void checkStatus() {
        // given
        QueueStatusResponse entered = queueService.enterQueue(3L, 300L);

        // when
        QueueStatusResponse status = queueService.checkStatus(3L, entered.token());

        // then
        assertThat(status.token()).isEqualTo(entered.token());
        assertThat(status.phase()).isEqualTo(QueuePhase.ALLOWED);
    }

    @DisplayName("유효하지 않은 토큰으로 상태를 조회하면 예외가 발생한다.")
    @Test
    void checkStatusWithInvalidToken() {
        // when & then
        assertThatThrownBy(() -> queueService.checkStatus(4L, "invalid-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 대기열 토큰입니다.");
    }

    @DisplayName("대기열 이탈 후 재진입하면 새로운 토큰을 발급받는다.")
    @Test
    void leaveQueueThenEnterAgain() {
        // given
        QueueStatusResponse first = queueService.enterQueue(5L, 500L);

        // when
        boolean removed = queueService.leaveQueue(5L, first.token()).removed();
        QueueStatusResponse second = queueService.enterQueue(5L, 500L);

        // then
        assertThat(removed).isTrue();
        assertThat(second.token()).isNotEqualTo(first.token());
    }
}
