package com.example.openticket.global.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class EventQueueManagerLoadTest {

    @DisplayName("동시에 150명이 진입하면 100명은 ALLOWED, 50명은 WAITING 상태여야 한다.")
    @Test
    void concurrentEnter_150Users_shouldSplitAllowedAndWaiting() throws InterruptedException {
        // given
        EventQueueManager manager = new EventQueueManager();
        Long eventId = 1L;
        int totalUsers = 150;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch readyLatch = new CountDownLatch(totalUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalUsers);
        Map<Long, String> userTokens = new ConcurrentHashMap<>();

        // when
        for (long userId = 1L; userId <= totalUsers; userId++) {
            long uid = userId;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    QueueEntry entry = manager.enter(eventId, uid);
                    userTokens.put(uid, entry.token());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        int allowedCount = 0;
        int waitingCount = 0;
        List<Integer> waitingPositions = new ArrayList<>();

        for (String token : userTokens.values()) {
            QueueStatus status = manager.check(eventId, token);
            if (status.phase() == QueuePhase.ALLOWED) {
                allowedCount++;
            } else {
                waitingCount++;
                waitingPositions.add(status.position());
            }
        }

        // then
        assertThat(userTokens).hasSize(totalUsers);
        assertThat(allowedCount).isEqualTo(100);
        assertThat(waitingCount).isEqualTo(50);
        assertThat(waitingPositions).contains(1, 50);
    }

    @DisplayName("동시 진입 테스트를 반복해도 ALLOWED는 최대 100명을 유지한다.")
    @RepeatedTest(20)
    void concurrentEnter_repeated_shouldNeverExceedActiveLimit() throws InterruptedException {
        concurrentEnter_150Users_shouldSplitAllowedAndWaiting();
    }

    @DisplayName("활성 사용자가 차차 빠져나가면 대기 사용자는 순서대로 승격된다.")
    @Test
    void waitingUser_shouldBePromoted_whenActiveUsersConsumeTokens() {
        // given
        EventQueueManager manager = new EventQueueManager();
        Long eventId = 1L;
        List<String> activeTokens = new ArrayList<>();

        for (long userId = 1L; userId <= 100; userId++) {
            activeTokens.add(manager.enter(eventId, userId).token());
        }

        QueueEntry waiting1 = manager.enter(eventId, 101L);
        QueueEntry waiting2 = manager.enter(eventId, 102L);
        QueueEntry waiting3 = manager.enter(eventId, 103L);

        assertThat(manager.check(eventId, waiting1.token()).position()).isEqualTo(1);
        assertThat(manager.check(eventId, waiting2.token()).position()).isEqualTo(2);
        assertThat(manager.check(eventId, waiting3.token()).position()).isEqualTo(3);

        // when
        boolean firstConsumed = manager.consumeActiveToken(eventId, activeTokens.get(0));
        boolean secondConsumed = manager.consumeActiveToken(eventId, activeTokens.get(1));
        boolean thirdConsumed = manager.consumeActiveToken(eventId, activeTokens.get(2));

        // then
        assertThat(firstConsumed).isTrue();
        assertThat(secondConsumed).isTrue();
        assertThat(thirdConsumed).isTrue();
        assertThat(manager.check(eventId, waiting1.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
        assertThat(manager.check(eventId, waiting2.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
        assertThat(manager.check(eventId, waiting3.token()).phase()).isEqualTo(QueuePhase.ALLOWED);
    }
}
