package com.example.openticket.infrastructure.queue.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.openticket.domain.queue.EventQueueManager;
import com.example.openticket.domain.queue.QueuePhase;
import com.example.openticket.domain.queue.QueueStatus;
import com.example.openticket.support.RedisTestSupport;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

@EnabledIfDockerAvailable
class RedisEventQueueManagerConcurrencyTest extends RedisTestSupport {

    @Autowired
    private EventQueueManager manager;

    @DisplayName("동시에 150명이 진입하면 100명은 ALLOWED, 50명은 WAITING 상태여야 한다.")
    @Test
    void concurrentEnter_150Users_shouldSplitAllowedAndWaiting() throws InterruptedException {
        Long eventId = 1L;
        int totalUsers = 150;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch readyLatch = new CountDownLatch(totalUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalUsers);
        Map<Long, String> userTokens = new ConcurrentHashMap<>();

        for (long userId = 1L; userId <= totalUsers; userId++) {
            long uid = userId;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    QueueStatus entry = manager.enter(eventId, uid);
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
        executorService.awaitTermination(3, TimeUnit.SECONDS);

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

        assertThat(userTokens).hasSize(totalUsers);
        assertThat(allowedCount).isEqualTo(100);
        assertThat(waitingCount).isEqualTo(50);
        assertThat(waitingPositions)
                .containsExactlyInAnyOrderElementsOf(
                        java.util.stream.IntStream.rangeClosed(1, 50).boxed().toList()
                );
    }

    @DisplayName("동시 진입 테스트를 반복해도 ALLOWED는 최대 100명을 유지한다.")
    @RepeatedTest(5)
    void concurrentEnter_repeated_shouldNeverExceedActiveLimit() throws InterruptedException {
        concurrentEnter_150Users_shouldSplitAllowedAndWaiting();
    }

    @DisplayName("100명의 활성 토큰을 동시에 소비하면 대기 중인 50명이 정확히 승격된다.")
    @Test
    void concurrentConsume_shouldPromoteExactlyWaitingUsers() throws InterruptedException {
        Long eventId = 2L;
        List<String> activeTokens = new ArrayList<>();

        for (long userId = 1L; userId <= 100; userId++) {
            activeTokens.add(manager.enter(eventId, userId).token());
        }

        List<QueueStatus> waitingEntries = new ArrayList<>();
        for (long userId = 101L; userId <= 150; userId++) {
            waitingEntries.add(manager.enter(eventId, userId));
        }

        // Verify all 50 are waiting
        for (QueueStatus entry : waitingEntries) {
            assertThat(manager.check(eventId, entry.token()).phase()).isEqualTo(QueuePhase.WAITING);
        }

        // Consume all 100 active tokens concurrently
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch readyLatch = new CountDownLatch(100);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(100);

        for (String token : activeTokens) {
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    manager.consumeActiveToken(eventId, token);
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
        executorService.awaitTermination(3, TimeUnit.SECONDS);

        // All 50 waiting users should now be ALLOWED
        int promotedCount = 0;
        for (QueueStatus entry : waitingEntries) {
            QueueStatus status = manager.check(eventId, entry.token());
            if (status.phase() == QueuePhase.ALLOWED) {
                promotedCount++;
            }
        }
        assertThat(promotedCount).isEqualTo(50);
    }
}
