package com.example.openticket.global.queue;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "queue.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryEventQueueManager implements EventQueueManager {

    private static final int MAX_ACTIVE_PER_EVENT = 100;
    private static final long ACTIVE_WINDOW_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<QueueEntry>> waitingQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, ActiveToken>> activeTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> sequenceCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> activeCounts = new ConcurrentHashMap<>();

    @Override
    public QueueEntry enter(Long eventId, Long userId) {
        evictExpiredTokensForEvent(eventId);

        QueueEntry activeEntry = findActiveEntry(eventId, userId);
        if (activeEntry != null) {
            return activeEntry;
        }

        QueueEntry waitingEntry = findWaitingEntry(eventId, userId);
        if (waitingEntry != null) {
            return waitingEntry;
        }

        QueueEntry newEntry = issueNewEntry(eventId, userId);
        if (tryActivate(eventId, newEntry)) {
            return newEntry;
        }
        addToWaitingQueue(eventId, newEntry);
        return newEntry;
    }

    @Override
    public QueueStatus check(Long eventId, String token) {
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

    @Override
    public boolean validate(Long eventId, String token) {
        return findValidActiveToken(eventId, token) != null;
    }

    @Override
    public boolean consumeActiveToken(Long eventId, String token) {
        ActiveToken activeToken = findValidActiveToken(eventId, token);
        if (activeToken == null) {
            return false;
        }

        ConcurrentHashMap<String, ActiveToken> eventTokens = activeTokens.get(eventId);
        if (!eventTokens.remove(token, activeToken)) {
            return false;
        }

        releaseActiveSlot(eventId);
        promoteWaiting(eventId);
        return true;
    }

    @Override
    public boolean leave(Long eventId, String token) {
        if (removeFromActive(eventId, token)) {
            return true;
        }
        return removeFromWaiting(eventId, token);
    }

    @Override
    public void promoteForEvent(Long eventId) {
        evictExpiredTokensForEvent(eventId);
        promoteWaiting(eventId);
    }

    private QueueEntry findActiveEntry(Long eventId, Long userId) {
        ConcurrentHashMap<String, ActiveToken> eventTokens = activeTokens
                .computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        for (var entry : eventTokens.entrySet()) {
            if (entry.getValue().userId().equals(userId)) {
                return new QueueEntry(userId, entry.getKey(), 0, entry.getValue().expiresAt() - ACTIVE_WINDOW_MS);
            }
        }
        return null;
    }

    private QueueEntry findWaitingEntry(Long eventId, Long userId) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueue = waitingQueues.computeIfAbsent(
                eventId, k -> new ConcurrentLinkedQueue<>()
        );
        for (QueueEntry entry : waitingQueue) {
            if (entry.userId().equals(userId)) {
                return entry;
            }
        }
        return null;
    }

    private QueueEntry issueNewEntry(Long eventId, Long userId) {
        long sequence = sequenceCounters.computeIfAbsent(eventId, k -> new AtomicLong(0))
                .incrementAndGet();
        String token = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        return new QueueEntry(userId, token, sequence, now);
    }

    private boolean tryActivate(Long eventId, QueueEntry entry) {
        ConcurrentHashMap<String, ActiveToken> eventTokens = activeTokens.computeIfAbsent(
                eventId, k -> new ConcurrentHashMap<>()
        );
        if (tryAcquireActiveSlot(eventId)) {
            ActiveToken newToken = createActiveToken(entry.userId());
            ActiveToken existing = eventTokens.putIfAbsent(entry.token(), newToken);
            if (existing == null) {
                return true;
            }
            releaseActiveSlot(eventId);
        }
        return false;
    }

    private void addToWaitingQueue(Long eventId, QueueEntry entry) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueue = waitingQueues
                .computeIfAbsent(eventId, k -> new ConcurrentLinkedQueue<>());
        waitingQueue.add(entry);
    }

    private QueueStatus findActiveStatus(Long eventId, String token) {
        ActiveToken activeToken = findValidActiveToken(eventId, token);
        if (activeToken == null) {
            return null;
        }
        return QueueStatus.allowed(token, calculateRemainingSeconds(activeToken));
    }

    private QueueStatus findWaitingStatus(Long eventId, String token) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueue = waitingQueues.get(eventId);
        if (waitingQueue != null) {
            int position = 1;
            for (QueueEntry entry : waitingQueue) {
                if (entry.token().equals(token)) {
                    return QueueStatus.waiting(token, position);
                }
                position++;
            }
        }
        return null;
    }

    private ActiveToken findValidActiveToken(Long eventId, String token) {
        ConcurrentHashMap<String, ActiveToken> eventTokens = activeTokens.get(eventId);
        if (eventTokens == null) {
            return null;
        }
        ActiveToken queueToken = eventTokens.get(token);
        if (queueToken == null || isExpired(queueToken)) {
            return null;
        }
        return queueToken;
    }

    private boolean removeFromActive(Long eventId, String token) {
        ConcurrentHashMap<String, ActiveToken> eventTokens = activeTokens.get(eventId);
        if (eventTokens == null) {
            return false;
        }
        ActiveToken removedToken = eventTokens.remove(token);
        if (removedToken == null) {
            return false;
        }
        releaseActiveSlot(eventId);
        promoteWaiting(eventId);
        return true;
    }

    private boolean removeFromWaiting(Long eventId, String token) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueue = waitingQueues.get(eventId);
        if (waitingQueue == null) {
            return false;
        }
        return waitingQueue.removeIf(entry -> entry.token().equals(token));
    }

    private void evictExpiredTokensForEvent(Long eventId) {
        ConcurrentHashMap<String, ActiveToken> eventTokens = this.activeTokens.get(eventId);
        if (eventTokens == null) {
            return;
        }
        int removedCount = 0;
        for (var entry : eventTokens.entrySet()) {
            if (isExpired(entry.getValue())
                    && eventTokens.remove(entry.getKey(), entry.getValue())) {
                removedCount++;
            }
        }
        releaseActiveSlots(eventId, removedCount);
    }

    private void promoteWaiting(Long eventId) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueue = waitingQueues.get(eventId);
        if (waitingQueue == null) {
            return;
        }
        ConcurrentHashMap<String, ActiveToken> eventTokens = activeTokens.computeIfAbsent(
                eventId, k -> new ConcurrentHashMap<>()
        );
        while (tryAcquireActiveSlot(eventId)) {
            QueueEntry nextEntry = waitingQueue.poll();
            if (nextEntry == null) {
                releaseActiveSlot(eventId);
                break;
            }
            ActiveToken promotedToken = createActiveToken(nextEntry.userId());
            ActiveToken existing = eventTokens.putIfAbsent(nextEntry.token(), promotedToken);
            if (existing != null) {
                releaseActiveSlot(eventId);
            }
        }
    }

    private boolean isExpired(ActiveToken token) {
        return token.expiresAt() < System.currentTimeMillis();
    }

    private long calculateRemainingSeconds(ActiveToken token) {
        return (token.expiresAt() - System.currentTimeMillis()) / 1000;
    }

    private ActiveToken createActiveToken(Long userId) {
        return new ActiveToken(userId, System.currentTimeMillis() + ACTIVE_WINDOW_MS);
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

    private void releaseActiveSlots(Long eventId, int count) {
        if (count <= 0) {
            return;
        }
        AtomicInteger activeCount = activeCounts.computeIfAbsent(eventId, k -> new AtomicInteger(0));
        activeCount.updateAndGet(current -> Math.max(0, current - count));
    }

    void reconcileActiveCount(Long eventId) {
        ConcurrentHashMap<String, ActiveToken> eventTokens = activeTokens.get(eventId);
        int actualCount = eventTokens == null ? 0 : eventTokens.size();
        AtomicInteger activeCount = activeCounts.computeIfAbsent(eventId, k -> new AtomicInteger(0));
        activeCount.set(Math.max(0, actualCount));
    }

    int activeCount(Long eventId) {
        AtomicInteger activeCount = activeCounts.get(eventId);
        return activeCount == null ? 0 : activeCount.get();
    }
}
