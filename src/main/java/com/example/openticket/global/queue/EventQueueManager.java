package com.example.openticket.global.queue;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventQueueManager {

    private static final int MAX_ACTIVE_PER_EVENT = 100;
    private static final long ACTIVE_WINDOW_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<QueueEntry>> waitingQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, QueueToken>> activeTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> sequenceCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> activeCounts = new ConcurrentHashMap<>();

    public QueueEntry enter(Long eventId, Long userId) {
        evictExpiredForEvent(eventId);

        QueueEntry activeEntry = findActiveEntry(eventId, userId);
        if (activeEntry != null) {
            return activeEntry;
        }

        QueueEntry waitingEntry = findWaitingEntry(eventId, userId);
        if (waitingEntry != null) {
            return waitingEntry;
        }

        return createNewEntry(eventId, userId);
    }

    public QueueStatus check(Long eventId, String token) {
        evictExpiredForEvent(eventId);
        promoteWaiting(eventId);

        QueueStatus activeStatus = findActiveStatus(eventId, token);
        if (activeStatus != null) {
            return activeStatus;
        }

        QueueStatus waitingStatus = findWaitingStatus(eventId, token);
        if (waitingStatus != null) {
            return waitingStatus;
        }

        throw new IllegalArgumentException("유효하지 않은 대기열 토큰입니다.");
    }

    private QueueEntry findActiveEntry(Long eventId, Long userId) {
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens
                .computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        for (var activeEntry : activeTokensByEvent.entrySet()) {
            if (activeEntry.getValue().userId().equals(userId)) {
                return new QueueEntry(userId, activeEntry.getKey(), 0, activeEntry.getValue().expiresAt() - ACTIVE_WINDOW_MS);
            }
        }
        return null;
    }

    private QueueEntry findWaitingEntry(Long eventId, Long userId) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueueByEvent = waitingQueues
                .computeIfAbsent(eventId, k -> new ConcurrentLinkedQueue<>());
        for (QueueEntry waitingEntry : waitingQueueByEvent) {
            if (waitingEntry.userId().equals(userId)) {
                return waitingEntry;
            }
        }
        return null;
    }

    private QueueEntry createNewEntry(Long eventId, Long userId) {
        long queueSequence = sequenceCounters.computeIfAbsent(eventId, k -> new AtomicLong(0))
                .incrementAndGet();
        String generatedToken = UUID.randomUUID().toString();
        long currentTimeMillis = System.currentTimeMillis();
        QueueEntry newWaitingEntry = new QueueEntry(userId, generatedToken, queueSequence, currentTimeMillis);

        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens
                .computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        if (tryAcquireActiveSlot(eventId)) {
            QueueToken newActiveToken = new QueueToken(userId, currentTimeMillis + ACTIVE_WINDOW_MS);
            QueueToken existing = activeTokensByEvent.putIfAbsent(generatedToken, newActiveToken);
            if (existing == null) {
                return newWaitingEntry;
            }
            releaseActiveSlot(eventId);
        }

        ConcurrentLinkedQueue<QueueEntry> waitingQueueByEvent = waitingQueues
                .computeIfAbsent(eventId, k -> new ConcurrentLinkedQueue<>());
        waitingQueueByEvent.add(newWaitingEntry);

        return newWaitingEntry;
    }

    private QueueStatus findActiveStatus(Long eventId, String token) {
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens.get(eventId);
        if (activeTokensByEvent != null) {
            QueueToken matchingActiveToken = activeTokensByEvent.get(token);
            if (matchingActiveToken != null) {
                long remainingSeconds = Math.max(0, (matchingActiveToken.expiresAt() - System.currentTimeMillis()) / 1000);
                return QueueStatus.allowed(token, remainingSeconds);
            }
        }
        return null;
    }

    private QueueStatus findWaitingStatus(Long eventId, String token) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueueByEvent = waitingQueues.get(eventId);
        if (waitingQueueByEvent != null) {
            int position = 1;
            for (QueueEntry waitingEntry : waitingQueueByEvent) {
                if (waitingEntry.token().equals(token)) {
                    return QueueStatus.waiting(token, position);
                }
                position++;
            }
        }
        return null;
    }

    public boolean validate(Long eventId, String token) {
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens.get(eventId);
        if (activeTokensByEvent == null) {
            return false;
        }
        QueueToken queueToken = activeTokensByEvent.get(token);

        return queueToken != null && queueToken.expiresAt() >= System.currentTimeMillis();
    }

    public boolean consumeActiveToken(Long eventId, String token) {
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens.get(eventId);
        if (activeTokensByEvent == null) {
            return false;
        }
        QueueToken consumedToken = activeTokensByEvent.remove(token);
        if (consumedToken == null) {
            return false;
        }
        releaseActiveSlot(eventId);
        promoteWaiting(eventId);
        return consumedToken.expiresAt() >= System.currentTimeMillis();
    }

    public boolean leave(Long eventId, String token) {
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens.get(eventId);
        if (activeTokensByEvent != null) {
            QueueToken removedActiveToken = activeTokensByEvent.remove(token);
            if (removedActiveToken != null) {
                releaseActiveSlot(eventId);
                promoteWaiting(eventId);
                return true;
            }
        }

        ConcurrentLinkedQueue<QueueEntry> waitingQueueByEvent = waitingQueues.get(eventId);
        if (waitingQueueByEvent == null) {
            return false;
        }

        QueueEntry matchingWaitingEntry = null;
        for (QueueEntry waitingEntry : waitingQueueByEvent) {
            if (waitingEntry.token().equals(token)) {
                matchingWaitingEntry = waitingEntry;
                break;
            }
        }
        if (matchingWaitingEntry == null) {
            return false;
        }

        return waitingQueueByEvent.remove(matchingWaitingEntry);
    }

    @Scheduled(fixedDelay = 30_000)
    public void evictExpired() {
        activeTokens.forEach((eventId, tokens) -> {
            evictExpiredForEvent(eventId);
            promoteWaiting(eventId);
        });
    }

    private void evictExpiredForEvent(Long eventId) {
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = this.activeTokens.get(eventId);
        if (activeTokensByEvent == null) {
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        int removedCount = 0;
        for (var activeEntry : activeTokensByEvent.entrySet()) {
            if (activeEntry.getValue().expiresAt() < currentTimeMillis
                    && activeTokensByEvent.remove(activeEntry.getKey(), activeEntry.getValue())) {
                removedCount++;
            }
        }
        releaseActiveSlots(eventId, removedCount);
    }

    private void promoteWaiting(Long eventId) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueueByEvent = waitingQueues.get(eventId);
        if (waitingQueueByEvent == null) {
            return;
        }
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens.computeIfAbsent(
                eventId, k -> new ConcurrentHashMap<>()
        );
        while (tryAcquireActiveSlot(eventId)) {
            QueueEntry nextWaitingEntry = waitingQueueByEvent.poll();
            if (nextWaitingEntry == null) {
                releaseActiveSlot(eventId);
                break;
            }
            long currentTimeMillis = System.currentTimeMillis();
            QueueToken promotedToken = new QueueToken(nextWaitingEntry.userId(), currentTimeMillis + ACTIVE_WINDOW_MS);
            QueueToken existing = activeTokensByEvent.putIfAbsent(nextWaitingEntry.token(), promotedToken);
            if (existing != null) {
                releaseActiveSlot(eventId);
            }
        }
    }

    private boolean tryAcquireActiveSlot(Long eventId) {
        AtomicInteger activeCount = activeCounts.computeIfAbsent(eventId, k -> new AtomicInteger(0));
        while (true) {
            int currentCount = activeCount.get();
            if (currentCount >= MAX_ACTIVE_PER_EVENT) {
                return false;
            }
            if (activeCount.compareAndSet(currentCount, currentCount + 1)) {
                return true;
            }
        }
    }

    private void releaseActiveSlot(Long eventId) {
        AtomicInteger activeCount = activeCounts.computeIfAbsent(eventId, k -> new AtomicInteger(0));
        activeCount.updateAndGet(currentCount -> Math.max(0, currentCount - 1));
    }

    private void releaseActiveSlots(Long eventId, int removedCount) {
        for (int i = 0; i < removedCount; i++) {
            releaseActiveSlot(eventId);
        }
    }

    void reconcileActiveCount(Long eventId) {
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens.get(eventId);
        int actualCount = activeTokensByEvent == null ? 0 : activeTokensByEvent.size();
        AtomicInteger activeCount = activeCounts.computeIfAbsent(eventId, k -> new AtomicInteger(0));
        activeCount.set(Math.max(0, actualCount));
    }

    int activeCount(Long eventId) {
        AtomicInteger activeCount = activeCounts.get(eventId);
        return activeCount == null ? 0 : activeCount.get();
    }

}
