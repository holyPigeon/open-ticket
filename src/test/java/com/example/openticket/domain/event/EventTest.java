package com.example.openticket.domain.event;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.openticket.domain.event.Event.EventBuilder;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EventTest {

    @DisplayName("이벤트의 시작 시간과 종료 시간이 유효할 경우, 예외가 발생하지 않는다.")
    @Test
    void createEventSuccess1() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime endAt = LocalDateTime.of(2027, 1, 1, 0, 0);

        // when & then
        assertThatNoException().isThrownBy(() -> {
            Event.builder()
                    .title("test event 1")
                    .category(Category.CONCERT)
                    .venue("test venue 1")
                    .startAt(startAt)
                    .endAt(endAt)
                    .build();
        });

    }

    @DisplayName("이벤트의 시작 시간이나 종료 시간이 null일 경우, 예외가 발생한다.")
    @ParameterizedTest(name = "startAt={0}, endAt={1}")
    @MethodSource("invalidDateTimes")
    void createEventFail1(LocalDateTime startAt, LocalDateTime endAt) {
        // given
        EventBuilder eventBuilder = Event.builder()
                .title("test event 1")
                .category(Category.CONCERT)
                .venue("test venue 1");

        // when & then
        assertThatThrownBy(() -> {
            eventBuilder
                    .startAt(startAt)
                    .endAt(endAt)
                    .build();
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작 시간 및 종료 시간은 null일 수 없습니다.");

    }

    @DisplayName("이벤트의 시작 시간이 종료 시간 이전이 아닌 경우, 예외가 발생한다.")
    @Test
    void createEventFail2() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2027, 1, 1, 0, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 1, 1, 0, 0);

        // when & then
        assertThatThrownBy(() -> {
            Event.builder()
                    .title("test event 1")
                    .category(Category.CONCERT)
                    .venue("test venue 1")
                    .startAt(startAt)
                    .endAt(endAt)
                    .build();
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작 시간은 종료 시간보다 이전이어야 합니다.");

    }

    static Stream<Arguments> invalidDateTimes() {
        return Stream.of(
                Arguments.of(null, java.time.LocalDateTime.of(2027, 1, 1, 0, 0)), // startAt null
                Arguments.of(java.time.LocalDateTime.of(2026, 1, 1, 0, 0), null), // endAt null
                Arguments.of(null, null)                                // 둘 다 null
        );
    }
}
