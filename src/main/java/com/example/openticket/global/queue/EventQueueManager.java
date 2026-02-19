package com.example.openticket.global.queue;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventQueueManager {

    private static final int MAX_ACTIVE_PER_EVENT = 100;
    private static final long ACTIVE_WINDOW_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<QueueEntry>> waitingQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, QueueToken>> activeTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> sequenceCounters = new ConcurrentHashMap<>();

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
        if (activeTokensByEvent.size() < MAX_ACTIVE_PER_EVENT) {
            activeTokensByEvent.put(generatedToken, new QueueToken(userId, currentTimeMillis + ACTIVE_WINDOW_MS));
        } else {
            ConcurrentLinkedQueue<QueueEntry> waitingQueueByEvent = waitingQueues
                    .computeIfAbsent(eventId, k -> new ConcurrentLinkedQueue<>());
            waitingQueueByEvent.add(newWaitingEntry);
        }

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
        if (consumedToken == null || consumedToken.expiresAt() < System.currentTimeMillis()) {
            return false;
        }
        promoteWaiting(eventId);
        return true;
    }

    @Scheduled(fixedDelay = 30_000)
    public void evictExpired() {
        activeTokens.forEach((eventId, tokens) -> {
            long now = System.currentTimeMillis();
            tokens.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
            promoteWaiting(eventId);
        });
    }

    private void evictExpiredForEvent(Long eventId) {
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = this.activeTokens.get(eventId);
        if (activeTokensByEvent == null) {
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        activeTokensByEvent.entrySet().removeIf(activeEntry -> activeEntry.getValue().expiresAt() < currentTimeMillis);
    }

    private void promoteWaiting(Long eventId) {
        ConcurrentLinkedQueue<QueueEntry> waitingQueueByEvent = waitingQueues.get(eventId);
        if (waitingQueueByEvent == null) {
            return;
        }
        ConcurrentHashMap<String, QueueToken> activeTokensByEvent = activeTokens.computeIfAbsent(
                eventId, k -> new ConcurrentHashMap<>()
        );
        long currentTimeMillis = System.currentTimeMillis();
        while (activeTokensByEvent.size() < MAX_ACTIVE_PER_EVENT) {
            QueueEntry nextWaitingEntry = waitingQueueByEvent.poll();
            if (nextWaitingEntry == null) {
                break;
            }
            activeTokensByEvent.put(nextWaitingEntry.token(), new QueueToken(nextWaitingEntry.userId(), currentTimeMillis + ACTIVE_WINDOW_MS));
        }
    }

}
