package com.example.openticket.infrastructure.queue.redis;

import com.example.openticket.domain.queue.EventQueueManager;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "queue.type", havingValue = "redis")
public class QueueSchedulerFacade {

    private static final String REGISTRY_KEY = "queue:events:registry";
    private static final String SCHEDULER_LOCK_KEY = "queue:scheduler:lock";

    private final EventQueueManager queueManager;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedDelayString = "${queue.promotion-interval-ms:5000}")
    public void promoteWaitingUsers() {
        RLock lock = redissonClient.getLock(SCHEDULER_LOCK_KEY);
        try {
            if (!lock.tryLock(0, TimeUnit.SECONDS)) {
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try (Cursor<String> cursor = redisTemplate.opsForSet()
                .scan(REGISTRY_KEY, ScanOptions.scanOptions().count(100).build())) {
            cursor.forEachRemaining(eventId -> {
                try {
                    queueManager.promoteForEvent(Long.parseLong(eventId));
                } catch (Exception e) {
                    log.warn("이벤트 대기열 승격 실패. eventId={}", eventId, e);
                }
            });
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
