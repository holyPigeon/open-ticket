package com.example.openticket.global.queue;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "queue.type", havingValue = "redis")
public class RedisEventQueueManager implements EventQueueManager {

    private static final String REGISTRY_KEY = "queue:events:registry";

    private final StringRedisTemplate redisTemplate;
    private final QueueProperties properties;

    private final DefaultRedisScript<List> enterScript;
    private final DefaultRedisScript<List> checkScript;
    private final DefaultRedisScript<Long> consumeScript;
    private final DefaultRedisScript<Long> leaveScript;
    private final DefaultRedisScript<Long> promoteScript;

    public RedisEventQueueManager(StringRedisTemplate redisTemplate, QueueProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;

        this.enterScript = loadScript("lua/queue_enter.lua", List.class);
        this.checkScript = loadScript("lua/queue_check.lua", List.class);
        this.consumeScript = loadScript("lua/queue_consume.lua", Long.class);
        this.leaveScript = loadScript("lua/queue_leave.lua", Long.class);
        this.promoteScript = loadScript("lua/queue_promote.lua", Long.class);
    }

    @Override
    public QueueEntry enter(Long eventId, Long userId) {
        String token = UUID.randomUUID().toString();
        long nowMs = System.currentTimeMillis();

        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId), tokensKey(eventId),
                usersKey(eventId), sequenceKey(eventId), REGISTRY_KEY
        );

        @SuppressWarnings("unchecked")
        List<String> result = redisTemplate.execute(enterScript, keys,
                userId.toString(),
                String.valueOf(properties.maxActivePerEvent()),
                String.valueOf(properties.activeWindowSeconds()),
                String.valueOf(nowMs),
                token,
                eventId.toString()
        );

        String returnedToken = result.get(1);
        return new QueueEntry(userId, returnedToken, 0, nowMs);
    }

    @Override
    public QueueStatus check(Long eventId, String token) {
        int metadataTtl = properties.activeWindowSeconds() * 2;

        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId),
                tokensKey(eventId), usersKey(eventId)
        );

        @SuppressWarnings("unchecked")
        List<String> result = redisTemplate.execute(checkScript, keys,
                token,
                String.valueOf(metadataTtl)
        );

        String phase = result.get(0);
        if ("INVALID".equals(phase)) {
            throw new IllegalArgumentException("유효하지 않은 대기열 토큰입니다.");
        }

        String returnedToken = result.get(1);
        int position = Integer.parseInt(result.get(2));
        long remainingSeconds = Long.parseLong(result.get(3));

        if ("ALLOWED".equals(phase)) {
            return QueueStatus.allowed(returnedToken, remainingSeconds);
        }
        return QueueStatus.waiting(returnedToken, position);
    }

    @Override
    public boolean validate(Long eventId, String token) {
        return redisTemplate.opsForHash().hasKey(activeKey(eventId), token);
    }

    @Override
    public boolean consumeActiveToken(Long eventId, String token) {
        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId),
                tokensKey(eventId), usersKey(eventId)
        );

        Long result = redisTemplate.execute(consumeScript, keys,
                token,
                String.valueOf(properties.activeWindowSeconds())
        );

        return result != null && result == 1L;
    }

    @Override
    public boolean leave(Long eventId, String token) {
        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId),
                tokensKey(eventId), usersKey(eventId)
        );

        Long result = redisTemplate.execute(leaveScript, keys,
                token,
                String.valueOf(properties.activeWindowSeconds())
        );

        return result != null && result == 1L;
    }

    @Override
    public void promoteForEvent(Long eventId) {
        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId),
                tokensKey(eventId), usersKey(eventId), REGISTRY_KEY
        );

        redisTemplate.execute(promoteScript, keys,
                String.valueOf(properties.maxActivePerEvent()),
                String.valueOf(properties.activeWindowSeconds()),
                eventId.toString()
        );
    }

    private String activeKey(Long eventId) {
        return "queue:events:" + eventId + ":active";
    }

    private String waitingKey(Long eventId) {
        return "queue:events:" + eventId + ":waiting";
    }

    private String tokensKey(Long eventId) {
        return "queue:events:" + eventId + ":tokens";
    }

    private String usersKey(Long eventId) {
        return "queue:events:" + eventId + ":users";
    }

    private String sequenceKey(Long eventId) {
        return "queue:events:" + eventId + ":sequence";
    }

    private static <T> DefaultRedisScript<T> loadScript(String path, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(resultType);
        return script;
    }
}
