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

        ConcurrentHashMap<String, QueueToken> active = activeTokens
                .computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());

        // Check if user already has an active token
        for (var entry : active.entrySet()) {
            if (entry.getValue().userId().equals(userId)) {
                return new QueueEntry(userId, entry.getKey(), 0, entry.getValue().expiresAt() - ACTIVE_WINDOW_MS);
            }
        }

        // Check if user is already waiting
        ConcurrentLinkedQueue<QueueEntry> queue = waitingQueues
                .computeIfAbsent(eventId, k -> new ConcurrentLinkedQueue<>());
        for (QueueEntry e : queue) {
            if (e.userId().equals(userId)) {
                return e;
            }
        }

        // Create new entry
        long seq = sequenceCounters
                .computeIfAbsent(eventId, k -> new AtomicLong(0))
                .incrementAndGet();
        String token = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        QueueEntry newEntry = new QueueEntry(userId, token, seq, now);

        if (active.size() < MAX_ACTIVE_PER_EVENT) {
            active.put(token, new QueueToken(userId, now + ACTIVE_WINDOW_MS));
        } else {
            queue.add(newEntry);
        }

        return newEntry;
    }

    public QueueStatus check(Long eventId, String token) {
        evictExpiredForEvent(eventId);
        promoteWaiting(eventId);

        ConcurrentHashMap<String, QueueToken> active = activeTokens.get(eventId);
        if (active != null) {
            QueueToken queueToken = active.get(token);
            if (queueToken != null) {
                long remaining = Math.max(0, (queueToken.expiresAt() - System.currentTimeMillis()) / 1000);
                return QueueStatus.allowed(token, remaining);
            }
        }

        ConcurrentLinkedQueue<QueueEntry> queue = waitingQueues.get(eventId);
        if (queue != null) {
            int position = 1;
            for (QueueEntry e : queue) {
                if (e.token().equals(token)) {
                    return QueueStatus.waiting(token, position);
                }
                position++;
            }
        }

        throw new IllegalArgumentException("유효하지 않은 대기열 토큰입니다.");
    }

    public boolean consumeActiveToken(Long eventId, String token) {
        ConcurrentHashMap<String, QueueToken> active = activeTokens.get(eventId);
        if (active == null) {
            return false;
        }
        QueueToken removed = active.remove(token);
        if (removed == null || removed.expiresAt() < System.currentTimeMillis()) {
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
        ConcurrentHashMap<String, QueueToken> active = activeTokens.get(eventId);
        if (active == null) {
            return;
        }
        long now = System.currentTimeMillis();
        active.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
    }

    private void promoteWaiting(Long eventId) {
        ConcurrentLinkedQueue<QueueEntry> queue = waitingQueues.get(eventId);
        if (queue == null) {
            return;
        }
        ConcurrentHashMap<String, QueueToken> active = activeTokens
                .computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        long now = System.currentTimeMillis();
        while (active.size() < MAX_ACTIVE_PER_EVENT) {
            QueueEntry head = queue.poll();
            if (head == null) {
                break;
            }
            active.put(head.token(), new QueueToken(head.userId(), now + ACTIVE_WINDOW_MS));
        }
    }

    public record QueueStatus(String token, QueuePhase phase, int position, long remainingSeconds) {

        static QueueStatus waiting(String token, int position) {
            return new QueueStatus(token, QueuePhase.WAITING, position, 0);
        }

        static QueueStatus allowed(String token, long remainingSeconds) {
            return new QueueStatus(token, QueuePhase.ALLOWED, 0, remainingSeconds);
        }
    }

    public enum QueuePhase {
        WAITING, ALLOWED
    }
}
